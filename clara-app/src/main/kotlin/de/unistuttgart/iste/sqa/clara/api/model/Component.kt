package de.unistuttgart.iste.sqa.clara.api.model

import de.unistuttgart.iste.sqa.clara.aggregation.platform.kubernetes.client.KubernetesPod

sealed interface AggregatedComponent {

    val name: Name

    @JvmInline
    value class Name(val value: String)

    data class External(override val name: Name, val domain: Domain) : AggregatedComponent

    sealed interface Internal : AggregatedComponent {

        val type: ComponentType?
        val version: Version?

        @JvmInline
        value class Version(val value: String) {

            override fun toString() = value
        }

        data class OpenTelemetryComponent(
            override val name: Name,
            override val type: ComponentType?,
            override val version: Version?,
            val domain: Domain,
            val paths: List<Path>,
        ) : Internal

        data class KubernetesComponent(
            override val name: Name,
            override val type: ComponentType?,
            override val version: Version?,
            override val namespace: Namespace,
            val ipAddress: IpAddress,
            val pods: List<KubernetesPod>,
        ) : Internal, Namespaced
    }
}

sealed interface Component {

    val name: Name

    @JvmInline
    value class Name(val value: String) {

        override fun toString() = value
    }

    data class InternalComponent(
        override val name: Name,
        val type: ComponentType?,
        val version: Version?,
        val namespace: Namespace?,
        val ipAddress: IpAddress?,
        val endpoints: Endpoints?,
    ) : Component {

        data class Endpoints(val domain: Domain, val paths: List<Path>)

        @JvmInline
        value class Version(val value: String) {

            override fun toString() = value
        }
    }

    data class ExternalComponent(
        override val name: Name,
        val domain: Domain,
    ) : Component
}
