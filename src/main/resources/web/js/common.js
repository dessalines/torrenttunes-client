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


  }).bind('typeahead:selected', function(e, data) {
    console.log(data);

    setSearchType(data);

    // console.log(searchId);
    // $('#search_id').val(searchId);

    $(this).submit();
  });

  // $('[name=search_input]').focus();

  // $('.tt-input').focus();
  setTimeout("$('[name=search_input]').focus();", 0);

  $("#search_form").submit(function(event) {
    var formData = $("#search_form").serializeArray();




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
  var mbidTypes = ['song_mbid', 'artist_mbid', 'album_mbid'];
  var searchTypeIndex = 0;
  for (i = 0; i < 3; i++) {
    searchTypeIndex = $.inArray(mbidTypes[i], Object.keys(data));
    console.log(searchTypeIndex);
    if (searchTypeIndex != -1) {
      break;
    }
  }

  var searchType = Object.keys(data)[searchTypeIndex];
  var searchMbid = data[searchType];

  if (searchType == 'song_mbid') {
    searchMbid = data['album_mbid'];
  }
  console.log(searchType);
  console.log(searchMbid);

  // remove the 3 classes first
  $("#search_form").removeClass('song-search-type');
  $("#search_form").removeClass('album-search-type');
  $("#search_form").removeClass('artist-search-type');


  if (searchType == 'song_mbid') {
    $("#search_form").addClass('song-search-type');
  } else if (searchType == 'album_mbid') {
    $("#search_form").addClass('album-search-type');
  } else if (searchType == 'artist_mbid') {
    $("#search_form").addClass('artist-search-type');
  }

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


function showPlaylist(id) {
  playlistPageTabID = id;
  // $('a[href="#homeTab"]').tab('show');
  $('a[href="#playlistPageTab"]').tab('show');
  console.log('showing ' + playlistPageTabID);
}


function setupTrackSelect() {
  $('.track-select').click(function(e) {
    console.log('track selected');
    // var full = this.id.split('_');
    var name = $(this).attr('name');
    var full = name.split('_');



    console.log(full);
    var option = full[0];
    var infoHash = full[1];

    console.log(option);
    console.log(infoHash);

    // radioMode.running = false;
    downloadOrFetchTrackObj(infoHash, option);


  });


  // console.log(library[0]);
  // console.log(library[id]);



}

function setupAddToPlaylist() {
  $('.add_to_playlist').click(function(e) {
    console.log('adding to playlist');
    // var full = this.id.split('_');
    var name = $(this).attr('name');
    var full = name.split('_');



    console.log(full);
    var option = full[0];
    var playlistId = full[1];

    var infoHash = $(this).closest('td').find('.track-select').attr('name').split('_')[1];
    simplePost('add_to_playlist/' + playlistId + "/" + infoHash, null, null, function() {
      console.log('Track ' + infoHash + ' added to playlist');
    });

  });


  // console.log(library[0]);
  // console.log(library[id]);
}



function setupTrackDelete() {
  $('.track-delete').click(function(e) {
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
      $('wrapper').tooltip('destroy');
    });

  });

}

function setupPlaylistTrackDelete() {
  $('.playlist-track-delete').click(function(e) {
    console.log('Deleting track');
    // var full = this.id.split('_');
    var name = $(this).attr('name');
    var full = name.split('_');



    console.log(full);
    var option = full[0];
    var playlistId = full[1];

    console.log(option);
    console.log(playlistId);

    var infoHash = $(this).closest('td').find('.track-select').attr('name').split('_')[1];
    simplePost('remove_from_playlist/' + playlistPageTabID + "/" + playlistId, null, null, function() {
      console.log('Track ' + infoHash + ' removed from playlist');
      $('wrapper').tooltip('destroy');
      setupPlaylistPageTab();
    });

  });

}
