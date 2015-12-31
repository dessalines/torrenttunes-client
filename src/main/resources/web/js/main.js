// mustache templates
var libraryTemplate = $('#library_template').html();
var uploadInfoTemplate = $('#upload_info_template').html();
var browseTemplate = $('#browse_template').html();

// The artist catalog pages
var artistCatalogTemplate = $('#artist_catalog_template').html();
var artistCatalogTemplate2 = $('#artist_catalog_template_2').html();
var topArtistAlbumsTemplate = $('#top_artist_albums_template').html();
var topArtistSongsTemplate = $('#top_artist_songs_template').html();
var allArtistAlbumsTemplate = $('#all_artist_albums_template').html();
var allArtistSongsTemplate = $('#all_artist_songs_template').html();

// The album catalog pages
var albumCatalogTemplate = $('#album_catalog_template').html();
var albumCatalogSongsTemplate = $('#album_catalog_songs_template').html();

// The home page template
var trendingAlbumsTemplate = $('#trending_albums_template').html();

// playlist templates
var playlistHomeTemplate = $('#playlist_home_template').html();
var playlistLeftTabTemplate = $('#playlist_left_tab_template').html();
var playlistPageTemplate = $('#playlist_page_template').html();
var addToPlaylistTemplate = $('#add_to_playlist_template').html();

var hrefToTrackObjMap = {};

// The browser playlists object
// The schema is {name: , tracks:[]}, {}, ...}
var playlists;

// the play queue
var library, playQueue;

// The radio station
var radioMode = {};

// the Upload polling var
var uploadInterval;

// The download status infohash map
var downloadStatusMap = {};

// This verifies that soundmanager has started
var player;
soundManager.onready(function() {
  console.log('started up');

  player = window.sm2BarPlayers[0];
  console.log(player.playlistController);
  console.log(player.dom);
  console.log(player.actions);
  // player.actions.play()
  // setupPlayQueue();
  // setCurrentTrackObj();
  player.playlistController.refresh();
  player.actions.next();
  player.actions.stop();
  setupPaths();

  loadPlayQueueFromLocalStorage();
  setupSortablePlayQueue();
  // unhide a few things if desktop
  if (!isMobile()) {
    $('.hide_on_mobile').removeClass('hide');
  }

});

$(document).ready(function() {


  keyboardShortcuts();

  loadPlaylistsFromLocalStorage();
  setupPlaylistLeftTab();
  setupPlayQueueBtn();
  setupHomeTab();
  setupUploadForm();
  setupUploadTable();
  setupBrowseTab();

  setupSettingsTab();
  setupPlaylistForm();
  setupDonate();

  setupTabs();

  setupUploadDownloadTotals();


});

function setupSortablePlayQueue() {
  // make the playlist div sortable
  $('#playlist_div').sortable({
    start: function(event, ui) {
      ui.item.startPos = ui.item.index();
    },
    update: function(event, ui) {
      saveReorderedPlayQueue(ui.item.startPos - 1, ui.item.index() - 1);
    }
  });
}

function setupUploadDownloadTotals() {
  setUploadDownloadTotals();
  // fetch every minute
  setInterval(function() {
    setUploadDownloadTotals();
  }, 60000);

}

function setUploadDownloadTotals() {
  console.log('Fetching upload/download totals');
  getJson('get_upload_download_totals').done(function(e) {
    $("#upload_download_totals").data('bs.tooltip').options.title = e;
  });
}

// test path : file:///home/tyler/git/torrenttunes-client/src/main/resources/web/html/main.html?artist=95e1ead9-4d31-4808-a7ac-32c3614c116b
// file:///home/tyler/git/torrenttunes-client/src/main/resources/web/html/main.html?album=e8c09b4e-33ae-368b-8f70-24b4e14fb9ad
// file:///home/tyler/git/torrenttunes-client/src/main/resources/web/html/main.html?song=23e0d0cc-f931-435e-8c53-8207dba4678a
function setupPaths() {
  // var url = getLastUrlPath();

  var artistMBID = getUrlParameter('artist');
  var albumMBID = getUrlParameter('album');
  var songMBID = getUrlParameter('song');
  var playlistStr = getUrlParameter('playlist');

  if (artistMBID != null) {
    showArtistPageV2(artistMBID);
  } else if (albumMBID != null) {
    showAlbumPage(albumMBID);
  } else if (songMBID != null) {


    getJson('get_song/' + songMBID, null, torrentTunesSparkService).done(function(e) {
      var track = JSON.parse(e);
      var infoHash = track['info_hash'];

      downloadOrFetchTrackObj(infoHash, 'play-now');
      var albumMBID = track['release_group_mbid'];
      showAlbumPage(albumMBID);

    });

  } else if (playlistStr != null) {
    setupLoadPlaylistFromPath(playlistStr);
  }

}

function setupLoadPlaylistFromPath(playlistStr) {


  var playlistTemp = JSON.parse(decodeURIComponent(playlistStr));
  var playlistName = playlistTemp['name'];

  var playlistIndex = findIndexInArray(playlists, 'name', playlistName);

  // First you need to see if you already have a playlist named that, 
  // If you do, just go there
  if (playlistIndex != null) {
    toastr.info('You already have a playlist named ' + playlistName);
    showPlaylist(playlistName);
  }
  // If it doesn't exist, you need to fetch the rest of the trackObjs,
  // and create the playlist
  else {

    var songMbids = playlistTemp['song_mbids'];
    console.log(songMbids);
    var playlist = {
      "name": playlistName,
      "tracks": []
    };


    // Add the playlist to the playlists
    var playlistIndex = playlists.push(playlist);

    // Setup the bars and dropdowns
    setupPlaylistLeftTab();
    setupPlaylistTab();
    addPlaylistDropdowns();

    deleteExtraFieldsFromPlaylists();


    (function loop(i) {
      if (i < songMbids.length) {


        var songMBID = songMbids[i];
        console.log(songMBID);

        getJson('get_song/' + songMBID, null, torrentTunesSparkService).done(function(e) {
          var trackObj = JSON.parse(e);
          console.log(trackObj);


          // The vars from those fields
          var infoHash = trackObj['info_hash'];
          var song_mbid = trackObj['song_mbid'];
          var file_path = trackObj['file_path'];
          var title = trackObj['title'];
          var artist_mbid = trackObj['artist_mbid'];
          var artist = trackObj['artist'];
          var duration_ms = trackObj['duration_ms'].toString();
          var release_group_mbid = trackObj['release_group_mbid'];
          var album = trackObj['album'];
          var seeders = trackObj['seeders'];

          var playlistTrackObj = {
            "album": album,
            "artist": artist,
            "artist_mbid": artist_mbid,
            "duration_ms": duration_ms,
            "info_hash": infoHash,
            "release_group_mbid": release_group_mbid,
            "seeders": seeders,
            "song_mbid": song_mbid,
            "title": title
          };

          playlists[playlistIndex - 1]['tracks'].push(playlistTrackObj);

          console.log(i);
          console.log(parseInt(songMbids.length));

          if (i == songMbids.length - 1) {
            savePlaylistsToLocalStorage();
            showPlaylist(playlistName);
          }

          loop(i + 1);
        });
      }

    })(0);


  }
}

function errorTest() {
  getJson('error_test').done(function(e) {

  });
}

function setupClickableArtistPlaying() {
  $('.artist_playing_clickable').click(function(e) {

    var mbid = $(this).attr('name');
    console.log(e);
    console.log(mbid);

    showArtistPageV2(mbid);

  });
}

function setupTabs() {

  $('a[data-toggle="tab"]').on('shown.bs.tab', function(e) {
    var tabId = $(e.target).attr("href");
    console.log(tabId);

    if (tabId == "#artistcatalogTab" || tabId == "#artistcatalog_main") {
      setupArtistCatalogTab();

    } else if (tabId == "#artistcatalog_album") {
      setupArtistCatalogAlbumTab();

    } else if (tabId == "#artistcatalog_compilation") {
      setupArtistCatalogCompilationTab();

    } else if (tabId == "#artistcatalog_song") {
      setupArtistCatalogSongTab();

    } else if (tabId == "#albumcatalogTab") {
      setupAlbumCatalogTab();

    } else if (tabId == "#browseTab") {
      // setupBrowseTab();
    } else if (tabId == "#homeTab") {
      setupHomeTab();
      clearParams();
    } else if (tabId == "#libraryTab") {
      setupLibrary();
    } else if (tabId == "#uploadTab") {

    } else if (tabId == "#playlistTab") {
      setupPlaylistTab();
    } else if (tabId == "#playlistPageTab") {
      setupPlaylistPageTab();
    }

  });
}

function setupPlaylistLeftTab() {



  // Mustache.parse(playlistLeftTabTemplate);
  // var rendered = Mustache.render(playlistLeftTabTemplate, playlists);
  // $('#playlist_left_tab_div').after(rendered);
  fillMustacheWithJson(playlists, playlistLeftTabTemplate, '#playlist_left_tab_div');


  // @deprecated
  // getJson('get_playlists').done(function(e) {
  //   $('li.playlist-left-tab-element').remove();
  //   var playlists = JSON.parse(e);
  //   console.log(playlists);
  //   Mustache.parse(playlistLeftTabTemplate);
  //   var rendered = Mustache.render(playlistLeftTabTemplate, playlists);
  //   console.log(rendered);

  //   $('#playlist_left_tab_div').after(rendered);


  // });
}

function setupPlaylistTab() {

  fillMustacheWithJson(playlists, playlistHomeTemplate, '#playlist_home_div');
  setupPlaylistDelete();
  deleteExtraFieldsFromPlaylists();
  // @deprecated
  // getJson('get_playlists').done(function(e) {
  //   var playLists = JSON.parse(e);
  //   fillMustacheWithJson(playLists, playlistHomeTemplate, '#playlist_home_div');
  //   setupPlaylistDelete();
  // });
}

function setupPlaylistPageTab() {

  var playlistIndex = findIndexInArray(playlists, 'name', playlistPageTabID);
  var playlist = playlists[playlistIndex];
  // console.log(playlist);

  fillMustacheWithJson(playlist, playlistPageTemplate, '#playlist_page_div');
  addPlaylistDropdowns();

  $('[data-toggle="tooltip"]').tooltip({
    container: 'body'
  });

  setupTrackSelect();

  setupPlaylistPlaySelect(playlist);

  // Setting up the resorting of the playlist
  $('#playlist_page_div tbody').sortable({
    placeholder: "ui-state-highlight",
    start: function(event, ui) {
      ui.item.startPos = ui.item.index();
    },
    update: function(event, ui) {

      saveReorderedPlaylist(playlistIndex, ui.item.startPos, ui.item.index());

      // setupPlaylistPageTab();

    }
  });

  setupPlaylistTrackDelete();

  deleteExtraFieldsFromPlaylists();

  replaceParams('playlist', buildPlaylistParams(playlist));

}

function setupPlaylistForm() {
  $('#create_playlist_form').bootstrapValidator({
      message: 'This value is not valid',
      excluded: [':disabled'],
      submitButtons: 'button[type="submit"]',

    })
    .on('success.form.bv', function(event) {

      event.preventDefault();

      var formData = $(this).serializeArray();

      console.log(formData);
      var name = formData[0]['value'];

      // Create the playlist object
      var playlistObj = {
        "name": name,
        "tracks": []
      };

      // Only add it if it doesn't exist
      var foundIndex = findIndexInArray(playlists, 'name', name);

      // Add it to the playlists array, keyed on the name
      if (foundIndex == null) {
        playlists.push(playlistObj);

        setupPlaylistLeftTab();
        setupPlaylistTab();

        savePlaylistsToLocalStorage();

        addPlaylistDropdowns();

        deleteExtraFieldsFromPlaylists();



      } else {
        toastr.error('Playlist ' + name + ' already exists');
      }





      // @deprecated
      // standardFormPost('create_playlist', "#create_playlist_form", null, null, function(id) {

      //   console.log('New playlist Created ' + id);
      //   showPlaylist(id);
      //   setupPlaylistLeftTab();
      // }, true);
    });
}

function setupSettingsTab() {

  $('#settingsForm').bootstrapValidator({
      message: 'This value is not valid',
      excluded: [':disabled'],
      submitButtons: 'button[type="submit"]',

    })
    .on('success.form.bv', function(event) {
      event.preventDefault();
      standardFormPost('save_settings', "#settingsForm");
    });

  getJson('get_settings').done(function(e) {
    var settings = JSON.parse(e);
    console.log(settings);

    $('input[name="max_upload_speed"]').val(settings['max_upload_speed']);
    $('input[name="max_download_speed"]').val(settings['max_download_speed']);
    $('input[name="max_cache_size_mb"]').val(settings['max_cache_size_mb']);
    $('input[name="storage_path"]').val(settings['storage_path']);


  });

  $('#uninstall_button').click(function() {
    simplePost('uninstall', null, false, null, true, localSparkService, '#uninstall_button');
    toastr.error("TorrentTunes Uninstalled");
    setTimeout(function() {
      open(location, '_self').close();
    }, 2000);

  });

  $('#clear_cache_button').click(function() {
    simplePost('clear_cache', null, false, null, true, localSparkService, '#clear_cache_button');
    toastr.success("Local song cache cleared");
  });

  $('#clear_database_button').click(function() {
    simplePost('clear_database', null, false, null, true, localSparkService, '#clear_database_button');
    toastr.success("Database Cleared");
  });


}




function setupBrowseTab() {
  $('#home_page_loading_div').removeClass('hide');
  getJson('get_artists', null, torrentTunesSparkService).done(function(e) {
    var artists = JSON.parse(e);
    console.log(artists);

    fillMustacheWithJson(artists, browseTemplate, '#browse_div');
    $('#home_page_loading_div').addClass('hide');
  });
}

function setupHomeTab() {

  getJson('get_trending_albums', null, torrentTunesSparkService).done(function(e) {
    var albums = JSON.parse(e);
    console.log(albums);

    fillMustacheWithJson(albums, trendingAlbumsTemplate, '#trending_albums_div');
  });

  getJson('get_trending_songs', null, torrentTunesSparkService).done(function(e) {
    var songs = JSON.parse(e);
    console.log(songs);

    fillMustacheWithJson(songs, libraryTemplate, '#trending_songs_div');
    $('[data-toggle="tooltip"]').tooltip({
      container: 'body'
    });
    addPlaylistDropdowns();
    setupTrackSelect();
    $('#home_page_div').removeClass('hide');
    $('#home_page_loading_div').addClass('hide');

  });

}


function setupAlbumCatalogTab() {

  $('#albumcatalogTab').addClass('hide');
  $('#home_page_loading_div').removeClass('hide');

  getJson('get_album/' + albumCatalogMBID, null, torrentTunesSparkService).done(function(e) {
    var album = JSON.parse(e);
    console.log(album);
    artistCatalogMBID = album['artist_mbid'];
    console.log('set the artist catalog MBID from the album = ' + artistCatalogMBID);

    fillMustacheWithJson(album, albumCatalogTemplate, '#album_catalog_div');

    $('#album_torrents_link').attr("href", torrentTunesSparkService + "get_album_zip/" +
      albumCatalogMBID);


    getJson('get_album_songs/' + albumCatalogMBID, null, torrentTunesSparkService).done(function(e) {
      var albumSongs = JSON.parse(e);
      console.log(albumSongs);

      fillMustacheWithJson(albumSongs, albumCatalogSongsTemplate, '#album_catalog_songs_div');
      $('[data-toggle="tooltip"]').tooltip({
        container: 'body'
      });
      addPlaylistDropdowns();
      setupTrackSelect();

      setupAlbumPlaySelect(albumSongs);
      $('#albumcatalogTab').removeClass('hide');
      $('#home_page_loading_div').addClass('hide');




    });

  });
}

function setupArtistCatalogSongTab() {

  $('#artistcatalogTab').addClass('hide');
  $('#home_page_loading_div').removeClass('hide');

  getJson('get_all_songs/' + artistCatalogMBID, null, torrentTunesSparkService).done(function(e) {
    var allArtistSongs = JSON.parse(e);
    console.log(allArtistSongs);

    fillMustacheWithJson(allArtistSongs, topArtistSongsTemplate, '#all_artist_songs_div');
    addPlaylistDropdowns();
    setupTrackSelect();

    $('#artistcatalogTab').removeClass('hide');
    $('#home_page_loading_div').addClass('hide');

  });
}

function setupArtistCatalogAlbumTab() {
  $('#artistcatalogTab').addClass('hide');
  $('#home_page_loading_div').removeClass('hide');

  getJson('get_all_albums/' + artistCatalogMBID, null, torrentTunesSparkService).done(function(e) {
    var allArtistAlbums = JSON.parse(e);
    console.log(allArtistAlbums);

    fillMustacheWithJson(allArtistAlbums, topArtistAlbumsTemplate, '#all_artist_albums_div');

    $('#artistcatalogTab').removeClass('hide');
    $('#home_page_loading_div').addClass('hide');
  });
}

function setupArtistCatalogCompilationTab() {
  $('#artistcatalogTab').addClass('hide');
  $('#home_page_loading_div').removeClass('hide');

  getJson('get_all_compilations/' + artistCatalogMBID, null, torrentTunesSparkService).done(function(e) {
    var allArtistAlbums = JSON.parse(e);
    console.log(allArtistAlbums);

    fillMustacheWithJson(allArtistAlbums, topArtistAlbumsTemplate, '#all_artist_compilations_div');


    $('#artistcatalogTab').removeClass('hide');
    $('#home_page_loading_div').addClass('hide');
  });
}

// if this tab was shown, that means a search was done for an artist
// do a query using the artistCatalogMBID to get the top songs, top albums,
// and all albums and songs
function setupArtistCatalogTab() {

  $('.tooltip').tooltip('destroy');
  $('#artistcatalogTab').addClass('hide');
  $('#home_page_loading_div').removeClass('hide');

  getJson('get_artist/' + artistCatalogMBID, null, torrentTunesSparkService).done(function(e) {
    var artistCatalog = JSON.parse(e);
    console.log(artistCatalog);

    fillMustacheWithJson(artistCatalog, artistCatalogTemplate, '#artist_catalog_div');
    fillMustacheWithJson(artistCatalog, artistCatalogTemplate2, '#artist_catalog_div_2');

    $('#discography_link').attr("href", torrentTunesSparkService + "get_artist_discography_zip/" +
      artistCatalogMBID);

    $('[data-toggle="tooltip"]').tooltip({
      container: 'body'
    });


    $('[name=artistcatalog_main]').click(function(e) {
      console.log('clicked');
      $('[name=artistcatalog_album], [name=artistcatalog_song]').removeClass('active');
    });

  });

  getJson('get_top_albums/' + artistCatalogMBID, null, torrentTunesSparkService).done(function(e) {
    var topArtistAlbums = JSON.parse(e);
    console.log(topArtistAlbums);

    fillMustacheWithJson(topArtistAlbums, topArtistAlbumsTemplate, '#top_artist_albums_div');
    $('#home_page_loading_div').addClass('hide');
    $('#artistcatalogTab').removeClass('hide');
  });

  getJson('get_top_songs/' + artistCatalogMBID, null, torrentTunesSparkService).done(function(e) {
    var topArtistSongs = JSON.parse(e);
    console.log(topArtistSongs);

    fillMustacheWithJson(topArtistSongs, topArtistSongsTemplate, '#top_artist_songs_div');
    addPlaylistDropdowns();
    setupTrackSelect();

    $('[data-toggle="tooltip"]').tooltip({
      container: 'body'
    });

  });
}

function setupPlayQueueBtn() {
  $('#play_queue_btn').click(function(e) {
    player.actions.menu();
  });
}

function setupUploadForm() {
  $('#upload_form').bootstrapValidator({
      message: 'This value is not valid',
      excluded: [':disabled'],
      submitButtons: 'button[type="submit"]'
    })
    .on('success.form.bv', function(event) {
      $('#upload_panel').removeClass('hide');
      event.preventDefault();

      // start the upload
      standardFormPost('upload_music_directory', "#upload_form", null, null, function() {
        console.log("upload complete");

        // stop polling the upload info
        clearInterval(uploadInterval);

        // reload the library page
        setupUploadTable();
        // setupLibrary();
        // setupPlayQueue();



      }, null, null, null, function() {
        clearInterval(uploadInterval);
      });


      // do polling of the information, post it to the front page
      uploadInterval = setInterval(function() {
        setupUploadTable();
      }, 1000);



    });
}

function setupUploadTable() {
  getJson('get_upload_info').done(function(e) {

    var uploadInfo = JSON.parse(e);
    if (e == "[]") {
      clearInterval(uploadInterval);
    } else {
      $('#upload_panel').removeClass('hide');
    }
    console.log(uploadInfo);

    fillMustacheWithJson(uploadInfo, uploadInfoTemplate, '#upload_info_div');

  });
}


function keyboardShortcuts() {


  $("html").on("keydown", function(e) {

    var searchBarIsFocused = $('.typeahead').is(':focus');
    var inputIsFocused = $('input').is(':focus');
    if (e.keyCode == 32 && !searchBarIsFocused && !inputIsFocused) {
      e.preventDefault();
      player.actions.play();
    }
  });


}

function addPlaylistDropdowns() {

  fillMustacheWithJson(playlists, addToPlaylistTemplate, ".add_to_playlist_class");
  if (playlists.length > 0) {

    Mustache.parse(addToPlaylistTemplate);
    var rendered = Mustache.render(addToPlaylistTemplate, playlists);
    // $(rendered).appendTo(".add_to_playlist_class");
    $(".add_to_playlist_class").html(rendered);

    setupAddToPlaylist();
  }

  // @deprecated
  // getJson('get_playlists').done(function(e) {

  //   var playlists = JSON.parse(e);
  //   console.log(playlists);
  //   // fillMustacheWithJson(playlists, addToPlaylistTemplate, ".add_to_playlist_class");
  //   if (playlists.length > 0) {

  //     Mustache.parse(addToPlaylistTemplate);
  //     var rendered = Mustache.render(addToPlaylistTemplate, playlists);
  //     $(rendered).appendTo(".add_to_playlist_class");

  //     setupAddToPlaylist();
  //   }

  // });

}

function setupLibrary() {


  getJson('get_library').done(function(e) {

    library = JSON.parse(e);
    console.log(library);

    fillMustacheWithJson(library, libraryTemplate, '#library_div');


    $('[data-toggle="tooltip"]').tooltip({
      container: 'body'
    });

    // $("[name=library_table]").tablesorter({
    //   sortList: [
    //     [2, 0],
    //     [4, 0]
    //   ]
    // });
    // $('.tablesorter').trigger('update');
    // setup the add/play buttons
    addPlaylistDropdowns();

    // TODO these may be a mistake
    setupTrackSelect();
    setupTrackDelete();

  });
}

function setupAlbumPlaySelect(albumSongs) {
  $('.play-album').click(function(e) {


    // downloads and plays them in order
    (function loop(i) {

      if (i < albumSongs.length) {

        var trackInfo = albumSongs[i];
        var infoHash = trackInfo['info_hash'];

        downloadOrFetchTrackObj(infoHash, 'play-last').done(function(e) {
          loop(i + 1);
        });

      }

    })(0);

  });
}

function setupPlaylistPlaySelect(playlist) {
  $('.play-playlist').click(function(e) {

    console.log(playlist);
    var tracks = playlist['tracks'];

    // downloads and plays them in order
    (function loop(i) {

      if (i < tracks.length) {

        var trackInfo = tracks[i];
        var infoHash = trackInfo['info_hash'];

        downloadOrFetchTrackObj(infoHash, 'play-last').done(function(e) {
          loop(i + 1);
        });

      }

    })(0);
  });
}


function updateDownloadStatusBar(infoHash) {
  // var tables = $('table');
  // console.log('tables = ' + tables);

  // var tableRows = tables[0].rows;
  // console.log(tableRows);


  getJson('get_torrent_progress/' + infoHash, true).done(function(percentageFloat) {

    var percentage = parseInt(percentageFloat * 100) + '%';

    // if (percentage == '0%') {
    //   percentage = '1%';
    // }

    console.log('percentage = ' + percentage);

    var rows = $("tr[data-info_hash='" + infoHash + "']");
    // console.log(rows);

    var numberOfTables = rows['length'];


    for (var i = 0; i < numberOfTables; i++) {
      var tr = rows[i];

      $(tr).css({
        // 'display': 'inline-table',
        // 'float': 'left',
        // 'clear': 'both',
        // 'height': '34px',
        // 'line-height': '34px',
        // 'white-space': 'nowrap',

        // 'width': '1070px',
        // 'display': 'inline-table',
        // 'position': 'relative',
        'background-image': 'url(../image/lblue.png)',
        'background-attachment': 'fixed',
        // 'background-image': 'none',
        // 'background-color': 'rgba(0,0,255,0)',
        'background-size': '1% 100%',
        // 'opacity': '0.6',
        /*your percentage is the first one (width), second one (100%) is for height*/
        'background-repeat': 'no-repeat',
        'transition': '0.5s',
        'left': '0'
      });


      $(tr).css({
        'background-size': percentage + ' 100%'
      });


      if (percentageFloat == 1.0) {

        $(tr).css({
          'background-image': 'none',
          'background-color': 'rgba(42,159,214,0.1)'
        });

      }


    }

    if (percentageFloat == 1.0) {
      console.log('Download finished for infohash: ' + infoHash);
      clearInterval(downloadStatusMap[infoHash]);
      setUploadDownloadTotals();
    }


  }).error(function(err) {
    // Stop going for it
    clearInterval(downloadStatusMap[infoHash]);
  });

}

function downloadOrFetchTrackObj(infoHash, option) {




  // now fetch or download the song
  var playButtonName = 'play-button_' + infoHash;

  // Updating the download status bar for that song
  downloadStatusMap[infoHash] = setInterval(function() {
    updateDownloadStatusBar(infoHash);
  }, 5000);

  return getJson('fetch_or_download_song/' + infoHash, null, null, playButtonName).done(function(e1) {

    var trackObj = JSON.parse(e1);

    replaceParams('song', trackObj['mbid']);

    // var id = parseInt(full[1]) - 1;
    var id = parseInt(trackObj['id']);


    if (option != 'play-radio') {
      toastr.success('Added ' + trackObj['artist'] + ' - ' + trackObj['title']);
    }


    if (option == 'play-now') {
      playNow(trackObj);
    } else if (option == 'play-button') {
      playNow(trackObj);
    } else if (option == 'play-next') {
      // add it to the playqueue
      addToQueueNext(trackObj);

    } else if (option == 'play-last') {
      // add it to the playqueue
      addToQueueLast(trackObj);
    } else if (option == 'play-radio') {
      createRadioStation(trackObj);
    }

    $('.sm2-bar-ui').removeClass('hide');


    playQueue.push(trackObj);
    savePlayQueueToLocalStorage();


    // Refresh the player
    player.playlistController.refresh();

    // post it to the DB to save it

    // simplePost('save_play_queue', JSON.stringify(playQueue), null, function() {
    //   // console.log('play queue saved');
    // }, null, null, null);

    simplePost('add_play_count/' + infoHash, null, null, function() {
      // console.log('play queue saved');
    }, true, torrentTunesSparkService, null);

  }).error(function(err) {
    simplePost('add_timeout_count/' + infoHash, null, null, function() {
      // console.log('play queue saved');
    }, true, torrentTunesSparkService, null);
  });
}


function addToQueueLast(trackObj) {

  var li = buildLiFromTrackObject(trackObj);

  // var index = $('.sm2-playlist-wrapper li').length - 1;
  var index = $("#playlist_div").children().length - 1;



  // $('.sm2-playlist-bd').append(li);
  $('#playlist_div').append(li);

  if (index <= 0) {
    index = 0;
    player.actions.prev();
  }

  setupTrackRemoveFromQueue();

  return index;

}

function addToQueueNext(trackObj) {

  var li = buildLiFromTrackObject(trackObj);

  var selected = $('#playlist_div .selected');
  var index = $('#playlist_div li').index(selected);

  console.log(index);

  $('#playlist_div li ').eq(index).after(li);

  setupTrackRemoveFromQueue();

  return index;


}





function playNow(trackObj) {
  // var li = buildLiFromTrackObject(trackObj);
  // $('.sm2-playlist-bd').prepend(li);

  // var item = player.playlistController.getItem(0);
  // console.log(item);
  // player.playlistController.select(item);
  player.playlistController.refresh();
  // player.actions.stop();

  // add it to the queue
  var index = addToQueueLast(trackObj);

  // player.actions.stop();
  $('.sm2-playlist-bd li').removeClass('selected');

  if (index != 0) {

    player.playlistController.playItemByOffset(index);
  } else {
    player.actions.prev();

    // Mobile play can only be started by touch(mobile rules of iOS and android)
    if (isMobile()) {

      delay(function() {
        player.actions.pause();
      }, 3000);

    }
  }
  // player.actions.play();
  setupClickableArtistPlaying();
}

function createRadioStation(trackObj) {
  radioMode.running = true;
  radioMode.queue = [];

  var artistMbid = trackObj['artist_mbid'];



  // First get the related songs:
  getJson('get_related_songs/' + artistMbid, null, torrentTunesSparkService).done(function(e) {

    var relatedSongs = JSON.parse(e);

    // play the first track
    playNow(trackObj);

    console.log(relatedSongs);
    (function loop(i) {
      if (i < relatedSongs.length) {
        var song = relatedSongs[i];
        console.log(song);

        var playType = 'play-last';


        // set a timeout for downloading: if it takes longer than 1 minute,
        // go on to the next track
        // setTimeout(function() {
        //   loop(i + 1);
        // }, 60000);

        downloadOrFetchTrackObj(song['info_hash'], playType).done(function(e2) {
          loop(i + 1);
        });
      }


    })(0);

  });



}

function buildLiFromTrackObject(trackObj) {
  // var encodedAudioFilePath = localSparkService + 'get_audio_file/' +
  //   encodeURIComponent(trackObj['file_path']);



  // var li = '<li><a href="' + encodedAudioFilePath + '"><b>' +
  //   trackObj['artist'] + '</b> - ' + trackObj['title'] + '</a></li>';

  var href = '"file://' + trackObj['file_path'] + '"';
  // var li = '<li><a href=' + href + '><b>' +
  //   '<span class="artist_playing_clickable" name="' + trackObj['artist_mbid'] + '">' +
  //   htmlDecode(htmlDecode(trackObj['artist'])) + '</span></b> - ' +
  //   htmlDecode(htmlDecode(trackObj['title'])) + '</a></li>';

  var li = '<li><div class="sm2-row">' +
    '<div class="sm2-col sm2-wide">' +
    '<a href=' + href + '><b>' +
    '<span class="artist_playing_clickable" name="' + trackObj['artist_mbid'] +
    '" mbid="' + trackObj['mbid'] + '">' +
    htmlDecode(htmlDecode(trackObj['artist'])) + '</span></b> - ' +
    htmlDecode(htmlDecode(trackObj['title'])) +
    '</a></div>' +
    '<div class="sm2-col">' +
    '<a class="sm2-icon sm2-exclude sm2-trash"' +
    'href="javascript:void(0)" title="Remove From Queue">' +
    '</a></div>' +
    '</div></li>';



  console.log(li);
  console.log(trackObj);
  // console.log(encodedAudioFilePath);

  hrefToTrackObjMap[href] = trackObj;





  return li;
}

function getCurrentTrackObj() {
  // Don't ask me fucking why
  var href = '"' + $('#playlist_div .selected > a:nth-child(1)').attr('href') + '"';
  return hrefToTrackObjMap[href];
}

function setCurrentTrackObj() {
  setInterval(function() {


    if (hrefToTrackObjMap.current == null ||
      hrefToTrackObjMap['current']['info_hash'] != getCurrentTrackObj()['info_hash']) {
      hrefToTrackObjMap.current = getCurrentTrackObj();
      console.log(hrefToTrackObjMap.current);
    }


  }, 15000);

}


function setupPlaylistDelete() {
  $('.playlist-delete').click(function(e) {
    // var full = this.id.split('_');
    var name = $(this).attr('name');
    var full = name.split('_');



    console.log(full);
    var option = full[0];
    var playlistName = full[1];

    console.log(option);
    console.log(playlistName);

    // remove the index from that array and save it
    var playlistIndex = findIndexInArray(playlists, 'name', playlistName);
    playlists.splice(playlistIndex, 1);
    savePlaylistsToLocalStorage();

    $('[name=' + name).parent().parent().remove();
    $('.tooltip').tooltip('destroy');

    setupPlaylistLeftTab();
    addPlaylistDropdowns();

    deleteExtraFieldsFromPlaylists();


    // @deprecated
    // simplePost('delete_playlist/' + playlistId, null, null, function() {
    //   // $('[name=' + name).tooltip('hide');
    //   // $('[name=' + name).parent().closest("a").remove();
    //   $('[name=' + name).parent().parent().remove();
    //   setupPlaylistLeftTab();

    // });

  });

}

function setupDonate() {
  var address = '14zPZaTFT8ipbi77FHw1uUEyCbGspWCzFX';
  var label = encodeURIComponent('TorrentTunes Donation');
  var amount = '0.02'
  var btcText = "bitcoin:" + address + '?label=' + label + '&amount=' + amount;
  $('.qrcode_a').attr('href', btcText);
  $('#qrcode').html('');
  $('#qrcode').qrcode({
    "render": "canvas",
    "width": 200,
    "height": 100,
    "fill": "#000",
    "background": "#FFF",
    "text": btcText,
  });
  $('#receive_address').html(address);

}

function loadPlaylistsFromLocalStorage() {

  // Load the playlists object from the cookies
  // var cookie = getCookie('playlists');
  // var cookie = Cookies.getJSON('playlists');

  var localstorage = localStorage.getItem('playlists');
  if (localstorage != undefined) {
    playlists = JSON.parse(localstorage);
    deleteExtraFieldsFromPlaylists();
  } else {
    console.log('set playlists');
    playlists = [];
  }

  // Check to make sure favorites exists, if it doesn't, create it
  var favoritesPlaylistIndex = findIndexInArray(playlists, 'name', 'Favorites');
  if (favoritesPlaylistIndex == null) {
    var playlist = {
      "name": "Favorites",
      "tracks": []
    };

    playlists.push(playlist);
    savePlaylistsToLocalStorage();
  }

  return playlists;

}

function loadPlayQueueFromLocalStorage() {
  var localstorage = localStorage.getItem('queue');
  if (localstorage != undefined) {
    playQueue = JSON.parse(localstorage);
  } else {
    console.log('set play queue');
    playQueue = [];
  }

  if (playQueue.length != 0) {
    $('.sm2-bar-ui').removeClass('hide');
  }

  // Fill the current tracks with the play queue
  playQueue.forEach(function(trackObj) {
    addToQueueLast(trackObj);
  });

  player.playlistController.refresh();
  player.actions.next();
  player.actions.stop();


}

function savePlaylistsToLocalStorage() {

  // var playlistsStr = JSON.stringify(playlists);
  // console.log('Writing playlists cookie ' + playlistsStr);
  // createCookie('playlists', playlistsStr);
  // $.cookie('playlists',playlistsStr);

  // Delete the special shit:
  deleteExtraFieldsFromPlaylists();

  // Cookies.set('playlists', playlists);
  localStorage.setItem('playlists', JSON.stringify(playlists));

}

function savePlayQueueToLocalStorage() {
  localStorage.setItem('queue', JSON.stringify(playQueue));
}


function deleteExtraFieldsFromPlaylists() {
  deleteMustacheFieldsFromObj(playlists);


  for (var i = 0; i < playlists.length; i++) {
    var playlist = playlists[i];

    deleteMustacheFieldsFromObj(playlist);

    // delete torrent_path if necessary
    var tracks = playlist['tracks'];

    for (var j = 0; j < tracks.length; j++) {
      var track = tracks[j];

      deleteTrackFieldsFromObj(track);
    }
  }
}

function deleteMustacheFieldsFromObj(myObj) {
  var mustacheFields = ["dateformat", "otherdateformat", "sparkUrl", "toFixed",
    "htmlDecode", "numToStars", "millisToMinutesAndSeconds", "seedersToType",
    "seedersToNum"
  ];

  for (var i = 0; i < mustacheFields.length; i++) {
    delete myObj[mustacheFields[i]];
  }
}

function deleteTrackFieldsFromObj(myObj) {
  var trackFields = ["album_coverart_thumbnail_large", "album_coverart_thumbnail_small",
    "album_coverart_url", "disc_number", "file_path", "plays", "torrent_path",
    "track_number", "year"
  ];

  for (var i = 0; i < trackFields.length; i++) {
    delete myObj[trackFields[i]];
  }

}

function buildPlaylistParams(playlist) {
  var tracks = playlist['tracks'];

  var str = '{"name":"' + playlist['name'] + '"';

  if (tracks.length > 0) {
    str += ',"song_mbids":[';
    str += '"' + tracks[0]['song_mbid'] + '"';
    for (var i = 1; i < tracks.length; i++) {
      var mbidStr = ',"' + tracks[i]['song_mbid'] + '"';
      str += mbidStr;
    }
  }

  str += ']}';

  return str;

}
