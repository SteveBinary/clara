# DNS

CLARA can analyze the logs of CoreDNS (the default Kubernetes DNS server) to discover communication of components via DNS queries.
For that feature to work correctly, it is crucial that the DNS server is configured to log DNS queries by enabling the `log` plugin.

```yaml title="An example ConfigMap for CoreDNS with the 'log' plugin enabled" hl_lines="9"
apiVersion: v1
kind: ConfigMap
metadata:
  name: coredns
  namespace: kube-system
data:
  Corefile: |
    .:53 {
        log
        errors
        health
        ready
        kubernetes cluster.local in-addr.arpa ip6.arpa {
          pods insecure
          fallthrough in-addr.arpa ip6.arpa
        }
        prometheus :9153
        forward . /etc/resolv.conf
        cache 30
        loop
        reload
        loadbalance
    }
```

!!! info "Other DNS servers"
    Your cluster might come with additional DNS servers to reduce the load.
    A prominent example is the [node-local-dns](https://kubernetes.io/docs/tasks/administer-cluster/nodelocaldns/) for caching DNS.
    There, you must also enable the `log` plugin.

!!! warning "Compatible DNS servers"
    Because CLARA analyzes the logged DNS queries,

    1. query logging must be activated
    2. the query logs must be compatible with the CoreDNS logs.

    Currently, CLARA analyzes all logs from the pods with the labels `k8s-app=kube-dns` or `k8s-app=node-local-dns` in the namespace `kube-system`.

## Managed Kubernetes cluster

!!! warning "Using a managed Kubernetes cluster from a service provider"
    When using a managed cluster from a service provider, changes to core components of Kubernetes might be not allowed directly.
    Please consult the documentation of your respective provider.

### DigitalOcean

For DigitalOcean, the correct way of enabling logging is to create a [special ConfigMap](https://docs.digitalocean.com/products/kubernetes/how-to/customize-coredns/):

``` yaml title="ConfigMap to activate query logging for CoreDNS in a Kubernetes cluster managed by DigitalOcean"
--8<-- "content/aggregation/platforms/kubernetes/dns/digital-ocean.coredns-override.configmap.yml"
```

## DNS debugging

As described in the [Kubernetes Documentation](https://kubernetes.io/docs/tasks/administer-cluster/dns-debugging-resolution/), you can use dnsutils to debug DNS resolution.
For CLARA, this is also a simple way of creating DNS queries explicitly and checking if CLARA detects the communication.
Just create a dnsutils-pod with the following manifest:

``` yaml
--8<-- "content/aggregation/platforms/kubernetes/dns/dnsutils.pod.yml"
```

Then you can use the following command to execute DNS queries:

```shell linenums="0"
kubectl exec -it dnsutils -n default -- nslookup google.com
```

Execute the following command to check the DNS server logs:

```shell linenums="0"
kubectl logs -l k8s-app=kube-dns -n kube-system
```

## Concept
The log DNS analysis uses the obtained information from the Kubernetes API to match the hostnames and ip-addresses in a DNS log to components of the cluster.  
An example log can look like this and provides disclosure about the source and target of a communication.
```
[INFO] 10.244.0.19:35065 - 3179 "A IN kubernetes.default.svc.cluster.local.svc.cluster.local. udp 72 false 512" NXDOMAIN qr,aa,rd 165 0.0000838s
```