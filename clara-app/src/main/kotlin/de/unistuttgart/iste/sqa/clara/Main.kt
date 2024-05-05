package de.unistuttgart.iste.sqa.clara

import de.unistuttgart.iste.sqa.clara.config.ClaraConfig
import java.util.concurrent.TimeUnit

fun main() {
    val config = ClaraConfig.loadFrom("/app/resources/config.yml")
    // Workaround to get the kube-config in the container at runtime
    // Runtime.getRuntime().exec("mkdir /root/.kube").waitFor(1, TimeUnit.SECONDS)
    // Runtime.getRuntime().exec("cp /app/resources/config /root/.kube/config").waitFor(1, TimeUnit.SECONDS)
    System.setProperty("kubeconfig", "/app/resources/config")

    val app = App(config)

    app.run()
}
