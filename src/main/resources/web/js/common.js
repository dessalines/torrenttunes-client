var artistCatalogMBID, albumCatalogMBID;


$(document).ready(function() {

  setupSearch();

});

function setupSearch() {


  var songURL = externalSparkService + 'song_search/%QUERY';
  var songList = new Bloodhound({
    datumTokenizer: Bloodhound.tokenizers.obj.whitespace('search'),
    // datumTokenizer: Bloodhound.tokenizers.whitespace,
    queryTokenizer: Bloodhound.tokenizers.whitespace,
    // prefetch: '../data/films/post_1960.json',
    remote: {
      url: songURL,
      wildcard: '%QUERY'
    }
  });

  var artistURL = externalSparkService + 'artist_search/%QUERY';
  var artistList = new Bloodhound({
    datumTokenizer: Bloodhound.tokenizers.obj.whitespace('search'),
    // datumTokenizer: Bloodhound.tokenizers.whitespace,
    queryTokenizer: Bloodhound.tokenizers.whitespace,
    // prefetch: '../data/films/post_1960.json',
    remote: {
      url: artistURL,
      wildcard: '%QUERY'
    }
  });

  var albumURL = externalSparkService + 'album_search/%QUERY';
  var albumList = new Bloodhound({
    datumTokenizer: Bloodhound.tokenizers.obj.whitespace('search'),
    // datumTokenizer: Bloodhound.tokenizers.whitespace,
    queryTokenizer: Bloodhound.tokenizers.whitespace,
    // prefetch: '../data/films/post_1960.json',
    remote: {
      url: albumURL,
      wildcard: '%QUERY'
    }
  });

  songList.initialize();
  artistList.initialize();
  albumList.initialize();


  var typeAhead = $('#search_box .typeahead').typeahead({
    hint: true,
    highlight: true,
    minLength: 1,
  }, {
    name: 'song_list',
    // display: 'song',
    displayKey: 'search',
    source: songList,
    templates: {
      header: '<h3 class="search-set">Songs</h3>'
    }
  }, {
    name: 'artist_list',
    // display: 'song',
    displayKey: 'search',
    source: artistList,
    templates: {
      header: '<h3 class="search-set">Artists</h3>'
    }
  }, {
    name: 'album_list',
    // display: 'song',
    displayKey: 'search',
    source: albumList,
    templates: {
      header: '<h3 class="search-set">Albums</h3>'
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


    } else if ($(this).hasClass('artist-search-type')) {
      console.log('its a artist');
      artistCatalogMBID = searchId;


      $('a[href="#artistcatalogTab"]').tab('show');
      $('a[href="#artistcatalog_main"]').tab('show');


    } else if ($(this).hasClass('album-search-type')) {
      console.log('its a album');
      albumCatalogMBID = searchId;
      $('a[href="#albumcatalogTab"]').tab('show');

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
