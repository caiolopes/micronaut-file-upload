# Micronaut File Upload

Just a reproducer project for the problem discussed in the following issue: https://github.com/micronaut-projects/micronaut-core/issues/4837

## Micronaut 3.1.3 Documentation

- [User Guide](https://docs.micronaut.io/3.1.3/guide/index.html)
- [API Reference](https://docs.micronaut.io/3.1.3/api/index.html)
- [Configuration Reference](https://docs.micronaut.io/3.1.3/guide/configurationreference.html)
- [Micronaut Guides](https://guides.micronaut.io/index.html)
---

## Feature mockito documentation

- [https://site.mockito.org](https://site.mockito.org)

## Feature http-client documentation

- [Micronaut HTTP Client documentation](https://docs.micronaut.io/latest/guide/index.html#httpClient)

## Feature assertj documentation

- [https://assertj.github.io/doc/](https://assertj.github.io/doc/)


## Steps to reproduce

Run a few times:

```
mvn test
```

You may want to run it like so:

```
mvn test -Dio.netty.leakDetection.level=paranoid
```

Until you get:

```
16:08:41.653 [default-nioEventLoopGroup-1-18] ERROR io.netty.util.ResourceLeakDetector - LEAK: ByteBuf.release() was not called before it's garbage-collected. See https://netty.io/wiki/reference-counted-objects.html for more information.
```
