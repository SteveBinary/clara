# Concept

CLARA itself is a data pipeline that collects information about an application deployed in a Kubernetes cluster from different data sources (i.e. the Kubernetes API, the Kubernetes internal DNS server and OpenTelemetry traces),
merges them, filters them and exports them to visually display the architecture of the examined application.

### Datapipeline
The datapipeline of CLARA consists of the four main steps:

- [Aggregation](../aggregation/index.md) 
- [Merging](../merging/index.md) 
- [Filtering](../filtering/index.md) 
- [Export](../export/index.md) 

![Architectural overview of CLARA](../_resources/CLARA_architecture.png "Architectural overview of CLARA")