package de.unistuttgart.iste.sqa.clara.export

import de.unistuttgart.iste.sqa.clara.api.export.Exporter
import de.unistuttgart.iste.sqa.clara.config.ExportConfig
import de.unistuttgart.iste.sqa.clara.config.ifEnabled
import de.unistuttgart.iste.sqa.clara.export.graphviz.GraphVizExporter
import de.unistuttgart.iste.sqa.clara.export.gropius.GropiusExporter
import io.github.oshai.kotlinlogging.KotlinLogging

class ExporterManager(exportConfig: ExportConfig) {

    private val log = KotlinLogging.logger {}

    val exporters: List<Exporter> = buildList {

        exportConfig.exporters?.graphviz?.ifEnabled { graphVizConfig ->
            val config = GraphVizExporter.Config(graphVizConfig.outputType, graphVizConfig.outputFile)
            add(GraphVizExporter(config))
            log.info { "Registered exporter: GraphViz" }
        }

        exportConfig.exporters?.gropius?.ifEnabled { gropiusConfig ->
            val config = GropiusExporter.Config(gropiusConfig.graphQLBackendUrl, gropiusConfig.graphQLBackendToken?.value)
            add(GropiusExporter(config))
            log.info { "Registered exporter: Gropius" }
        }
    }
}
