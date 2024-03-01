package de.unistuttgart.iste.sqa.clara.merge

import arrow.core.Either
import arrow.core.right
import de.unistuttgart.iste.sqa.clara.aggregation.platform.kubernetes.aggregators.toCommunication
import de.unistuttgart.iste.sqa.clara.api.merge.ComponentMerger
import de.unistuttgart.iste.sqa.clara.api.merge.MergeFailure
import de.unistuttgart.iste.sqa.clara.api.model.*

class DynamicComponentMerger : ComponentMerger {

    // First we need to filter the returned component types (Otel, Dns, etc)
    // Next we define a strict merging hierarchy: 1. DNS 2. Otel, 3. SBOM, 4. ?)
    // We take the one with the highest hierarchy as baseline
    // then we iterate over the first two service group's components and try to merge.
    // thereby, it is important that we somehow keep the reference of the original objects in the merged ones to merge the communications later.
    // then the next component group is merged into the previous result
    // in then end we have one list of components containing all information found

    // next we need to merge communications.
    // communications take generic source, target types and thus should work with the result.
    // we just need to make sure, that the merged component can still be matched, as the communications hold a reference to their object
    override fun merge(components: List<AggregatedComponent>, communications: List<AggregatedCommunication>): Pair<List<Either<MergeFailure, Component>>, List<Either<MergeFailure, Communication>>> {

        if (components.isEmpty()) {
            return Pair(emptyList(), emptyList())
        }

        // For now, we only have those two service types.
        val mergedComponents = compareAndMergeComponents(
            components,
            AggregatedComponent.Internal.KubernetesComponent::class.java,
            AggregatedComponent.Internal.OpenTelemetryComponent::class.java,
        )

        // TODO filter communications where target/source does not exist anymore
        return Pair(mergedComponents, communications.map { it.toCommunication().right() })
    }

    private fun <B, C> compareAndMergeComponents(
        aggregatedComponents: List<AggregatedComponent>,
        baseComponentType: B,
        compareComponentType: C,
    ): List<Either<MergeFailure, Component>> {

        val baseComponents = aggregatedComponents.filter { it::class.java == baseComponentType }
        val compareComponents = aggregatedComponents.filter { it::class.java == compareComponentType }.toMutableList()

        if (baseComponents.isEmpty() || compareComponents.isEmpty()) {
            return aggregatedComponents.map { Either.Right(it.toComponent()) }
        }

        val mergedComponents = mutableListOf<Either.Right<Component>>()

        baseComponents.forEach { baseComponent ->
            val compareComponent = compareComponents.find {
                compareComponents(baseComponent, it)
            }
            if (compareComponent != null) {
                mergedComponents.add(Either.Right(mergeComponents(baseComponent, compareComponent)))
                compareComponents.remove(compareComponent)
            }
        }
        return mergedComponents
    }

    // TODO more complex comparison based on attributes if names do not match
    private fun compareComponents(baseComponent: AggregatedComponent, compareComponent: AggregatedComponent): Boolean =
        baseComponent.name == compareComponent.name

    private fun mergeComponents(baseComponent: AggregatedComponent, compareComponent: AggregatedComponent): Component {
        return when {
            baseComponent::class.java == AggregatedComponent.Internal.KubernetesComponent::class.java &&
                    compareComponent::class.java == AggregatedComponent.Internal.OpenTelemetryComponent::class.java ->
                mergeKubernetesComponentWithOpenTelemetryComponent(
                    baseComponent as AggregatedComponent.Internal.KubernetesComponent,
                    compareComponent as AggregatedComponent.Internal.OpenTelemetryComponent
                )

            else -> throw UnsupportedOperationException("We can not merge those components yet (${baseComponent::class.java.name} and ${compareComponent::class.java.name}).")
        }
    }

    // TODO this is very rudimentary yet
    private fun mergeKubernetesComponentWithOpenTelemetryComponent(
        baseComponent: AggregatedComponent.Internal.KubernetesComponent,
        compareComponent: AggregatedComponent.Internal.OpenTelemetryComponent,
    ): Component.InternalComponent {
        return Component.InternalComponent(
            name = Component.Name(baseComponent.name.value),
            namespace = baseComponent.namespace,
            ipAddress = baseComponent.ipAddress,
            endpoints = Component.InternalComponent.Endpoints(compareComponent.domain, compareComponent.paths),
        )
    }
}

private fun AggregatedComponent.toComponent(): Component {
    return when (this) {
        is AggregatedComponent.External -> Component.ExternalComponent(
            name = Component.Name(this.name.value),
            domain = this.domain
        )

        is AggregatedComponent.Internal.KubernetesComponent -> Component.InternalComponent(
            name = Component.Name(this.name.value),
            namespace = this.namespace,
            ipAddress = this.ipAddress,
            endpoints = null,
        )

        is AggregatedComponent.Internal.OpenTelemetryComponent -> Component.InternalComponent(
            name = Component.Name(this.name.value),
            namespace = null,
            ipAddress = null,
            endpoints = Component.InternalComponent.Endpoints(this.domain, this.paths)
        )
    }
}
