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

  songList.initialize();


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

  }).bind('typeahead:selected', function(e, data) {
    console.log(data);

    // add a class for the type
    var searchId;
    if (data['name'] != null) {
      $("#search_form").addClass('song-search-type');
      searchId = data['id'];

    } else if (data['title'] != null) {
      $("#search_form").addClass('product-search-type');
      searchId = data['product_id'];
    } else if (data['shop_name'] != null) {
      $("#search_form").addClass('shop-search-type');
      searchId = data['id'];
    }

    $('#search_id').val(searchId);

    $(this).submit();
  });

  // $('[name=search_input]').focus();

  // $('.tt-input').focus();
  setTimeout("$('[name=search_input]').focus();", 0)

  $("#search_form").submit(function(event) {
    var formData = $("#search_form").serializeArray();



    // var classList = document.getElementsByName('creators_list').className.split(/\s+/);
    // console.log(classList);
    console.log(formData);
    var searchId = formData[0].value;
    var searchString = formData[1].value;


    var redirectUrl;
    if ($(this).hasClass('song-search-type')) {
      console.log('its a song');
      redirectUrl = "/song/" + searchId;
    } else if ($(this).hasClass('product-search-type')) {
      console.log('its a product');
      redirectUrl = "/product/" + searchId;
    } else if ($(this).hasClass('shop-search-type')) {
      console.log('its a shop');
      redirectUrl = "/shop/" + searchId;
    }
    // console.log(searchString);

    if (redirectUrl == null) {
      redirectUrl = '/browse';
    }
    // window.location.replace(redirectUrl);

    event.preventDefault();
  });
}
