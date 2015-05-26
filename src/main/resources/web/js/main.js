// mustache templates
var libraryTemplate = $('#library_template').html();
var uploadInfoTemplate = $('#upload_info_template').html();

// the play queue
var library, playQueue = [];

// the Upload polling var
var uploadInterval;

// This verifies that soundmanager has started
var player;
soundManager.onready(function() {
  console.log('started up');

  player = window.sm2BarPlayers[0];
  console.log(player.playlistController);
  console.log(player.dom);
  console.log(player.actions);
  // player.actions.play();
});

$(document).ready(function() {
  keyboardShortcuts();

  setupLibrary();
  setupPlayQueue();
  setupUploadForm();
  setupPlayQueueBtn();


});

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
      event.preventDefault();

      // start the upload
      standardFormPost('upload_music_directory', "#upload_form", null, null, function() {
        console.log("upload complete");

        // stop polling the upload info
        clearInterval(uploadInterval);

        // reload the library page
        setupUploadTable();
        setupLibrary();
        setupPlayQueue();



      });


      // do polling of the information, post it to the front page
      uploadInterval = setInterval(function() {
        setupUploadTable();
      }, 500);



    });
}

function setupUploadTable() {
  getJson('get_upload_info').done(function(e) {

    var uploadInfo = JSON.parse(e);
    if (e == "[]") {
      clearInterval(uploadInterval);
    }
    console.log(uploadInfo);

    fillMustacheWithJson(uploadInfo, uploadInfoTemplate, '#upload_info_div');

  });
}


function keyboardShortcuts() {

  $("html").on("keydown", function(e) {
    if (e.keyCode == 32) {
      e.preventDefault();
      player.actions.play();
    }
  });

}


function setupLibrary() {
  getJson('get_library').done(function(e) {

    library = JSON.parse(e);
    console.log(library);

    fillMustacheWithJson(library, libraryTemplate, '#library_div');

    $("#library_table").tablesorter({
    	sortList: [[2,0],[4,0]]
    }); 
    // $('.tablesorter').trigger('update');
    // setup the add/play buttons
    setupTrackSelect(library);

  });
}

function setupTrackSelect(library) {
  $('.track-select').click(function(e) {
    var full = this.id.split('_');
    var option = full[0];
    var id = parseInt(full[1])-1;

    console.log(option);
    console.log(id);

    // now get the object
    // console.log(library[0]);
    console.log(library[id]);

    if (option == 'play-now') {
      playNow(library[id]);
    } else if (option == 'play-button') {
      playNow(library[id]);
    } else if (option == 'play-next') {
      // add it to the playqueue
      addToQueueNext(library[id]);

    } else if (option == 'play-last') {
      // add it to the playqueue
      addToQueueLast(library[id]);
    }

    $('.sm2-bar-ui').removeClass('hide');

    playQueue.push(library[id]);

    console.log(playQueue);

    // Refresh the player
    player.playlistController.refresh();

    // post it to the DB to save it
    simplePost('save_play_queue', JSON.stringify(playQueue), null, function() {
      // console.log('play queue saved');
    }, null, null, null);

  });

}

function setupPlayQueue() {
  // Load it from the DB
  getJson('get_play_queue').done(function(e) {
    playQueue = JSON.parse(e);
    console.log("play queue = " + playQueue);

    if (playQueue != "") {
      $('.sm2-bar-ui').removeClass('hide');
      // Now reload the bottom bar playlist



    }
    playQueue.forEach(function(trackObj) {
      addToQueueLast(trackObj);
    });
    player.playlistController.refresh();
    player.actions.next();
    player.actions.stop();


  });

}

function addToQueueLast(trackObj) {

  var li = buildLiFromTrackObject(trackObj);

  // var index = $('.sm2-playlist-wrapper li').length - 1;
  var index = $("#playlist_div").children().length - 1;
  console.log(index);



  // $('.sm2-playlist-bd').append(li);
  $('#playlist_div').append(li);

  if (index <= 0) {
    index = 0;

  }


  return index;

}

function addToQueueNext(trackObj) {

  var li = buildLiFromTrackObject(trackObj);

  var selected = $('#playlist_div .selected');
  var index = $('#playlist_div li').index(selected);

  console.log(index);

  $('#playlist_div li ').eq(index).after(li);
  return index;


}





function playNow(trackObj) {
  // var li = buildLiFromTrackObject(trackObj);
  // $('.sm2-playlist-bd').prepend(li);

  // var item = player.playlistController.getItem(0);
  // console.log(item);
  // player.playlistController.select(item);
  // player.playlistController.refresh();
  // player.actions.stop();

  // add it to the queue
  var index = addToQueueLast(trackObj);

  // player.actions.stop();
  $('.sm2-playlist-bd li').removeClass('selected');
  console.log(index);
  if (index != 0) {

    player.playlistController.playItemByOffset(index);
  } else {
    player.actions.prev();
    // player.actions.play(); 
  }
  // player.actions.play();

}

function buildLiFromTrackObject(trackObj) {
  var li = '<li><a href="' + trackObj['file_path'] + '"><b>' +
    trackObj['artist'] + '</b> - ' + trackObj['title'] + '</a></li>';

  return li;
}
