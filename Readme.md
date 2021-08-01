## Tiny-KTorrent

This is a simple BitTorrent client written in Kotlin. The architecture is largely based on [the c++ implementation by ss16118](https://github.com/ss16118/torrent-client-cpp/)

Here are some main reference blog posts:

- [Building a BitTorrent client from the ground up in Go](https://blog.jse.li/posts/torrent/)
- [重复造轮子的喜悦：从零开始用C++写一个最基础的BitTorrent客户端](https://zhuanlan.zhihu.com/p/386437665)
- [Kotlin Bencode Decoding and Encoding](https://blog.matthewbrunelle.com/projects/2018/07/29/kotlin-bencode.html)

Currently the code works only on seeds from a site which is called AcademicTorrents (a seed from this site is provided in repo), on single file. It doesn't support pause and resume, 
multi-file or seeding.

### Known Issues

- Status display is not showing
- While connecting to a certain types of peers, the handshake will fail (the socket tries to read 68 bytes but it received an EOF)
- Not supporting compact format in tracker's response