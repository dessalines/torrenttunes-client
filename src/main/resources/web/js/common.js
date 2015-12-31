var artistCatalogMBID, albumCatalogMBID, playlistPageTabID;

var songPlayerTemplate = $('#song_player_template').html();


$(document).ready(function() {

  // history.replaceState(null, null,"file:///URL Rewrite Example", null);

  // window.location.hash="derp";
  setupSearch();

  $('[data-toggle="tooltip"]').tooltip();

  // closing the window functions
  setupWindowClose();

});

function setupWindowClose() {
  window.onbeforeunload = function() {
    console.log('derp');


    simplePost('power_off', null, false, null, true);
    return "TorrentTunes has been powered off.";
    // return null;
  };
}

function setupSearch() {




  var artistURL = torrentTunesSparkService + 'artist_search/%QUERY';
  var artistList = new Bloodhound({
    queryTokenizer: Bloodhound.tokenizers.whitespace,
    datumTokenizer: Bloodhound.tokenizers.whitespace,
    // prefetch: '../data/films/post_1960.json',
    remote: {
      url: artistURL,
      wildcard: '%QUERY'
    }
  });

  var albumURL = torrentTunesSparkService + 'album_search/%QUERY';
  var albumList = new Bloodhound({
    queryTokenizer: Bloodhound.tokenizers.whitespace,
    datumTokenizer: Bloodhound.tokenizers.whitespace,
    // prefetch: '../data/films/post_1960.json',
    remote: {
      url: albumURL,
      wildcard: '%QUERY'
    }
  });

  var songURL = torrentTunesSparkService + 'song_search/%QUERY';
  var songList = new Bloodhound({
    queryTokenizer: Bloodhound.tokenizers.whitespace,
    datumTokenizer: Bloodhound.tokenizers.whitespace,
    // prefetch: '../data/films/post_1960.json',
    remote: {
      url: songURL,
      wildcard: '%QUERY',
    },
    // local: ['dog', 'pig', 'moose', 'dog2', 'dog4'],
  });


  artistList.initialize();
  albumList.initialize();
  songList.initialize();


  var typeAhead = $('#search_box .typeahead').typeahead({
    hint: true,
    highlight: true,
    minLength: 3,
  }, {
    name: 'artist_list',
    displayKey: 'search_artist',
    source: artistList,
    templates: {
      header: '<h3 class="search-set">Artists</h3>',
      suggestion: function(context) {
        return Mustache.render('<div>{{{search_artist}}} </div>', context);
      }
    }
  }, {
    name: 'album_list',
    displayKey: 'search_album',
    source: albumList,
    templates: {
      header: '<h3 class="search-set">Albums</h3>',
      suggestion: function(context) {
        return Mustache.render('<div>{{{search_album}}} </div>', context);
      }
    }
  }, {
    name: 'song_list',
    displayKey: 'search_song',
    source: songList,
    templates: {
      header: '<h3 class="search-set">Songs</h3>',
      // suggestion: function(context) {
      //   var ret = Mustache.render(songPlayerTemplate, context);
      //   // setupTrackSelect();
      //   // $('a', ret).on('click', function() {
      //   //   alert('derp');
      //   // });
      //   return ret;
      // }
      suggestion: function(context) {
        return Mustache.render('<div>{{{search_song}}} </div>', context);
      }

    }


  }).bind('typeahead:selected', function(e, data, name) {
    console.log(e);
    console.log(data);
    console.log(name);

    setSearchType(data);

    // console.log(searchId);
    // $('#search_id').val(searchId);

    $(this).submit();
  }).bind('typeahead:render', function(e) {

    $('#search_form').parent().find('.tt-selectable:first').addClass('tt-cursor');

  });

  // $('[name=search_input]').focus();

  // $('.tt-input').focus();
  setTimeout("$('[name=search_input]').focus();", 0);

  $("#search_form").submit(function(event) {
    var formData = $("#search_form").serializeArray();

    hideKeyboard($('[name=search_input]'));

    // var classList = document.getElementsByName('creators_list').className.split(/\s+/);
    // console.log(classList);
    console.log(formData);
    var searchId = formData[0].value;
    var searchString = formData[1].value;



    // This removes the left tab considered active, so that it can be reshown with 
    // updated data
    $('li.active a[data-toggle="tab"]').parent().removeClass('active');

    var redirectUrl;
    if ($(this).hasClass('song-search-type')) {
      console.log('its a song');

      albumCatalogMBID = searchId;
      showAlbumPage(albumCatalogMBID);

    } else if ($(this).hasClass('artist-search-type')) {
      console.log('its a artist');
      artistCatalogMBID = searchId;
      showArtistPage();


    } else if ($(this).hasClass('album-search-type')) {
      console.log('its a album');
      albumCatalogMBID = searchId;
      showAlbumPage(albumCatalogMBID)

    }
    // console.log(searchString);


    event.preventDefault();
  });
}

function setSearchType(data) {
  // add a class for the type
  var searchTypes = ['search_song', 'search_artist', 'search_album'];
  var searchTypeIndex = 0;
  for (i = 0; i < 3; i++) {
    searchTypeIndex = $.inArray(searchTypes[i], Object.keys(data));
    console.log(searchTypeIndex);
    if (searchTypeIndex != -1) {
      break;
    }
  }


  var searchType = Object.keys(data)[searchTypeIndex];



  // remove the 3 classes first
  $("#search_form").removeClass('song-search-type');
  $("#search_form").removeClass('album-search-type');
  $("#search_form").removeClass('artist-search-type');


  var searchMbid;
  if (searchType == 'search_song') {
    searchMbid = data['release_group_mbid'];
    $("#search_form").addClass('song-search-type');
  } else if (searchType == 'search_album') {
    searchMbid = data['mbid'];
    $("#search_form").addClass('album-search-type');
  } else if (searchType == 'search_artist') {
    searchMbid = data['artist_mbid'];
    $("#search_form").addClass('artist-search-type');
  }

  console.log(searchType);
  console.log(searchMbid);



  $('#search_id').val(searchMbid);
}



function showArtistPage() {

  replaceParams('artist', artistCatalogMBID);


  // $('a[href="#artistcatalogTab"]').hide();
  $('#left_tab li.active').removeClass('active');
  $('#artistcatalogTab').removeClass('active');

  $('#artistcatalogTab').addClass('hide');
  $('a[href="#artistcatalogTab"]').tab('show');
  $('a[href="#artistcatalog_main"]').tab('show');


}

function showArtistPageV2(artistMBID) {
  artistCatalogMBID = artistMBID;
  showArtistPage();
}

function showAlbumPage(releaseMBID) {
  replaceParams('album', releaseMBID);
  albumCatalogMBID = releaseMBID;
  $('#albumcatalogTab').addClass('hide');
  $('a[href="#albumcatalogTab"]').tab('show');
}


function showPlaylist(name) {
  playlistPageTabID = name;
  setupPlaylistPageTab();

  deleteExtraFieldsFromPlaylists();
  // $('a[href="#homeTab"]').tab('show');
  $('a[href="#playlistPageTab"]').tab('show');
  console.log('showing ' + playlistPageTabID);
}


function setupTrackSelect() {
  $('.track-select').unbind('click').click(function(e) {
    console.log('track selected');
    // var full = this.id.split('_');
    var name = $(this).attr('name');
    var full = name.split('_');



    console.log(full);
    var option = full[0];
    var infoHash = full[1];

    console.log(option);
    console.log(infoHash);

    downloadOrFetchTrackObj(infoHash, option);


  });


  // console.log(library[0]);
  // console.log(library[id]);
}

function setupTrackRemoveFromQueue() {

  $('.sm2-trash').unbind('click').click(function(e) {

    var mbid = $(this).closest('li').find('.artist_playing_clickable').attr('mbid');

    console.log(mbid);

    var queueIndex = findIndexInArray(playQueue, 'mbid', mbid);

    console.log('mbid = ' + mbid);
    console.log('queueIndex = ' + queueIndex);

    playQueue.splice(queueIndex, 1);

    savePlayQueueToLocalStorage();

    console.log('removing track from queue');
    var li = $(this).closest('li');
    // console.log(li);
    li.remove();

    player.playlistController.refresh();


  });
}

function setupAddToPlaylist() {
  $('.add_to_playlist').unbind('click').click(function(e) {
    console.log('adding to playlist');
    // var full = this.id.split('_');
    var name = $(this).attr('name');
    var full = name.split('][');



    console.log(full);
    var option = full[0];
    var playlistName = full[1];

    var closest = $(this).closest('td').find('.track-select');

    // The vars from those fields
    var infoHash = closest.attr('info_hash');
    var song_mbid = closest.attr('song_mbid');
    var file_path = closest.attr('file_path');
    var title = closest.attr('title');
    var artist_mbid = closest.attr('artist_mbid');
    var artist = closest.attr('artist');
    var duration_ms = closest.attr('duration_ms');
    var release_group_mbid = closest.attr('release_group_mbid');
    var album = closest.attr('album');
    var seeders = closest.attr('seeders');



    var playlistIndex = findIndexInArray(playlists, 'name', playlistName);
    console.log('playlist index = ' + playlistIndex);

    var playlist = playlists[playlistIndex];
    var tracks = playlist['tracks'];

    // Make sure that infohash doesn't already exist
    var trackIndex = findIndexInArray(tracks, 'info_hash', infoHash);

    if (trackIndex == null) {
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


      deleteExtraFieldsFromPlaylists();

      tracks.push(playlistTrackObj);

      savePlaylistsToLocalStorage();

      toastr.success(playlistTrackObj['title'] + ' added to playlist ' + playlist['name']);
    } else {
      toastr.error('Track already exists in playlist');
    }

    // @deprecated
    // simplePost('add_to_playlist/' + playlistId + "/" + infoHash, null, null, function() {
    //   console.log('Track ' + infoHash + ' added to playlist');
    // });

  });


  // console.log(library[0]);
  // console.log(library[id]);
}

function saveReorderedPlaylist(playlistIndex, oldIndex, newIndex) {
  var playlist = playlists[playlistIndex];
  var tracks = playlist['tracks'];

  console.log('old = ' + oldIndex + ' new = ' + newIndex);

  // move the tracks
  tracks.splice(newIndex, 0, tracks.splice(oldIndex, 1)[0]);

  savePlaylistsToLocalStorage();
}

function saveReorderedPlayQueue(oldIndex, newIndex) {

  console.log('old = ' + oldIndex + ' new = ' + newIndex);

  // move the tracks
  playQueue.splice(newIndex, 0, playQueue.splice(oldIndex, 1)[0]);


  savePlayQueueToLocalStorage();
}

function setupTrackDelete() {
  $('.track-delete').unbind('click').click(function(e) {
    console.log('track selected for delete');
    // var full = this.id.split('_');
    var name = $(this).attr('name');
    var full = name.split('_');



    console.log(full);
    var option = full[0];
    var infoHash = full[1];

    console.log(option);
    console.log(infoHash);

    simplePost('delete_song/' + infoHash, null, null, function() {

      $('[name=' + name).closest("tr").remove();
      $('.tooltip').tooltip('destroy');
    });

  });

}

function setupPlaylistTrackDelete() {
  $('.playlist-track-delete').unbind('click').click(function(e) {
    console.log('Deleting track');
    // var full = this.id.split('_');
    var name = $(this).attr('name');
    var full = name.split('_');



    console.log(full);
    var option = full[0];
    var playlistName = full[1];

    console.log(option);
    console.log(playlistName);

    var infoHash = $(this).closest('td').find('.track-select').attr('name').split('_')[1];

    var playlistIndex = findIndexInArray(playlists, 'name', playlistName);
    console.log('playlist index = ' + playlistIndex);

    var playlist = playlists[playlistIndex];
    var tracks = playlist['tracks'];

    var trackIndex = findIndexInArray(tracks, 'info_hash', infoHash);

    tracks.splice(trackIndex, 1);

    $('.tooltip').tooltip('destroy');
    $('[data-info_hash="' + infoHash + '"').remove();


    savePlaylistsToLocalStorage();

    toastr.success('Track Removed');

    // @deprecated
    // simplePost('remove_from_playlist/' + playlistPageTabID + "/" + playlistId, null, null, function() {
    //   console.log('Track ' + infoHash + ' removed from playlist');
    //   $('wrapper').tooltip('destroy');
    //   setupPlaylistPageTab();
    // });

  });

}
