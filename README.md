This is ByteVault — a Google Drive–like object storage system I’ve been building to understand how large-scale storage systems actually work internally rather than just consuming cloud APIs.

The main focus of the project is scalable file storage, chunking large files, streaming downloads efficiently, and designing the backend architecture in a way that could later evolve into a distributed storage system.

The project is built using Spring Boot and follows a layered architecture.

I’ve separated the application into controllers, services, repositories, entities, and storage logic to keep concerns isolated and maintainable.

The upload flow works like this:

the client uploads a file → the backend processes it → splits it into chunks → stores those chunks separately → and maintains metadata for reconstruction later.

One engineering decision I made was chunking files instead of storing everything as a single binary blob.

Chunking improves scalability and is similar to how distributed storage systems handle large files internally.

This also creates flexibility for future distributed replication because chunks can later be stored across different storage nodes independently.

For downloads, I specifically wanted to avoid loading entire files into memory because that becomes inefficient for large files.

So instead of reconstructing the complete file in RAM, I implemented streaming using piped streams.
This makes the system much more memory efficient and closer to production-style backend handling for large objects
I focused a lot on maintainability and separation of concerns while building this.

Business logic is isolated in services, controllers remain lightweight, and storage handling is abstracted cleanly.
Some future improvements I’m planning are distributed replication, async processing, Dockerized deployment, and object deduplication.

Overall, this project was mainly driven by curiosity around backend systems and storage internals, and it helped me think much more deeply about scalability, memory efficiency, and system design beyond just building CRUD applications.