[TorrentTunes](http://torrenttunes.ml) &mdash; A free, BitTorrent-based music streaming service.
==========
![](http://img.shields.io/version/0.7.6.png?color=green)


[TorrentTunes](http://torrenttunes.ml) is an open-source, [BitTorrent-based](https://en.wikipedia.org/wiki/BitTorrent) music service, like Rdio or Spotify, where you can stream any music, or share your own library of music with others.

Each individual song is tagged using [MusicBrainz](http://musicbrainz.org/), and shared as a *.torrent* file using the client's built-in, [LibTorrent-based](http://www.libtorrent.org/) BitTorrent client, on publicly available trackers.

TorrentTunes works on Windows, Mac OS X, and Linux, and also has a web client at [TorrentTunes.ml](http://torrenttunes.ml).

Suggest features, new music, or post bugs [here](https://github.com/tchoulihan/torrenttunes-client/issues/), or on the subreddit [r/torrenttunes](http://www.reddit.com/r/torrenttunes).

[:arrow_down: Install TorrentTunes](https://github.com/tchoulihan/torrenttunes-launcher/releases/download/1.0.0/torrenttunes-launcher.jar)


[Change log](https://github.com/tchoulihan/torrenttunes-client/releases)

## Features include
* A complete music streaming application including Artist and album pages, playlists, and settings.
* Fully categorized songs based on IDv3 tags and song duration with [MusicBrainz](http://musicbrainz.org/), with the [Musicbrainz-tagger](https://github.com/tchoulihan/musicbrainz-tagger).
* A [LibTorrent-based](http://www.libtorrent.org/) BitTorrent client.
* Song, album, and artist sharing via URLs: [David Bowie - Diamond Dogs](http://torrenttunes.ml/?album=0dc4835d-b21a-3612-bac6-ab1e782a1396)
* Playlist sharing via URLS: [Sample playlist](http://torrenttunes.ml/?playlist={%22name%22:%22Sample_Playlist%22,%22song_mbids%22:[%2294bed552-376f-492d-8e87-f470aa7fda0c%22,%228972c8e0-4e44-43b5-9f58-fd7c5d91f304%22,%221d3e9ebe-0738-4f5a-8488-14a985c84d7c%22]})



## Screenshots:
<img src="http://i.imgur.com/vSA20Q5.png">
<img src="http://i.imgur.com/VGrtFSg.png">
<img src="http://i.imgur.com/j3OmWe8.png">



## Installation
### Requirements
- Java 8
- If behind a router, make sure bittorrent has enough open ports forwarded to your local IP address.

Download and double click the jar, located [here](https://github.com/tchoulihan/torrenttunes-launcher/releases/download/1.0.0/torrenttunes-launcher.jar)

Torrenttunes will install, create system shortcuts for Windows, Mac, or Debian-based Linux distributions, and load up in a browser window.

## Building from scratch

To build TorrentTunes, run the following commands:
```
git clone https://github.com/tchoulihan/torrenttunes-client
cd torrenttunes-client
chmod +x install.sh

# This script does a maven install, and java -jar command
./install.sh
```
## Command line client

To download and run [TorrentTunes](http://torrenttunes.ml) from the command line(the output is written to `log.out`):

```
wget https://github.com/tchoulihan/torrenttunes-client/releases/download/0.7.6/torrenttunes-client.jar
nohup java -jar torrenttunes-client.jar &> log.out &
```

To scan your music directory:

`curl localhost:4568/share_directory --data "/home/user-dir/Music/A Music Dir"`

To stop the torrenttunes service:

`curl -XPOST localhost:4568/power_off`

## Support 
If you'd like to help fund me to keep developing [TorrentTunes](http://torrenttunes.ml), you can donate [here](https://www.bountysource.com/teams/torrenttunes-client), or directly with bitcoin:

[bitcoin:14zPZaTFT8ipbi77FHw1uUEyCbGspWCzFX](bitcoin:14zPZaTFT8ipbi77FHw1uUEyCbGspWCzFX)

![bitcoin:14zPZaTFT8ipbi77FHw1uUEyCbGspWCzFX](http://i.imgur.com/RRGOvl2.png)

## Bugs and feature requests
Have a bug or a feature request? If your issue isn't [already listed](https://github.com/tchoulihan/torrenttunes-client/issues/), then open a [new issue here](https://github.com/tchoulihan/torrenttunes-client/issues/new).

## Contributing
Start with getting familiar with the [schema](http://ondras.zarovi.cz/sql/demo/?keyword=torrenttunes-client) first, then create some issues and we can start working on them. 


## Feature requests / todos
* TODO

