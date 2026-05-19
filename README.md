ByteVault is basically a Google Drive–style storage system I started building because I wanted to understand how large-scale storage systems actually work internally instead of just using cloud APIs blindly.

The core idea is handling large files efficiently using chunking, streaming, replication, and distributed storage concepts.

When a user uploads a file, the backend splits it into smaller chunks instead of storing it as one huge file. Those chunks are then distributed across multiple storage nodes, and replicas are also maintained for fault tolerance in case a node fails or a chunk gets corrupted.

I built it using Java 21 and Spring Boot with a layered backend architecture, so controllers, services, repositories, and storage logic are all separated cleanly.

One thing I specifically focused on was memory efficiency during downloads. Instead of rebuilding the entire file in memory, the system streams chunks and reassembles them on the fly using piped streams, which is much closer to how production systems handle large objects.

The project started as a simple file upload system, but gradually evolved into something much more systems-oriented involving chunk management, replication strategies, streaming I/O, and distributed storage concepts.

Right now I’m working on improving replication handling, checksum validation for corruption detection, and making the storage nodes more distributed and fault tolerant. Later I also want to add async replication, Dockerized deployment, and cloud-style object storage features.