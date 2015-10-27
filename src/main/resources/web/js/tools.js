var localSparkService = 'http://localhost:4568/';
var torrentTunesSparkService = 'http://torrenttunes.ml/';
var externalSparkService = 'http://derp:4568/';

// var externalSparkService = 'http://127.0.0.1:4567/';

// This means you are running a seller machine, and everything should be using localhost



var sparkServiceObj = {
  sparkUrl: localSparkService
};


var pageNumbers = {};
// var cookie_path_name = "/";



function getJson(shortUrl, noToast, sparkService, name) {
  noToast = (typeof noToast === "undefined") ? false : noToast;
  sparkService = (typeof sparkService === "undefined" || sparkService == null) ? localSparkService : sparkService;
  name = (typeof name === "undefined") ? null : name;

  var url;

  url = sparkService + shortUrl;

  console.log(url);
  return simpleAjax(url, noToast, name);


}

function simpleAjax(url, noToast, name) {

  if (name != null) {
    var btn = $('[name="' + name + '"]');
    var prevText = btn.html();

    btn.html('<i class="fa fa-circle-o-notch fa-spin fa-fw"></i>');


  }

  // Loading
  // btn.button('loading');

  return $.ajax({
    type: "GET",
    url: url,
    cache: true,
    // ifModified:true,
    xhrFields: {
      withCredentials: true
    },
    // data: seriesData, 
    success: function(data, status, xhr) {
      if (name != null) {
        btn.html(prevText);
      }
    },
    error: function(request, status, error) {
      if (!noToast) {

        if (request.status == 0) {
          toastr.error("TorrentTunes hasn't been started yet...");
        } else {
          toastr.error(request.responseText);
        }
      }

      if (name != null) {
        console.log('replacing button text');
        btn.html(prevText);
      }
    }
  });
}





function fillSimpleText(url, divId) {
  var url = localSparkService + url // the script where you handle the form input.
  return $.ajax({
    type: "GET",
    url: url,
    cache: true,
    xhrFields: {
      withCredentials: true
    },
    // data: seriesData, 
    success: function(data, status, xhr) {
      // console.log(data);
      $(divId).html(data);


    },
    error: function(request, status, error) {

      toastr.error(request.responseText);
    }
  });
}

function fillStatusText(url, divId) {
  var url = localSparkService + url // the script where you handle the form input.
  var intervalID = setInterval(function() {
    $.ajax({
      type: "GET",
      url: url,
      cache: true,
      xhrFields: {
        withCredentials: true
      },
      // data: seriesData, 
      success: function(data, status, xhr) {
        $(divId).html(data);
        if (data == "Sync Complete") {
          clearInterval(intervalID);
          $('.sync-incomplete').fadeOut(1500);
          $('.wallet-btn').removeClass('disabled');
          fillSimpleText('wallet/balance', '#balance');
        }
      },
      error: function(request, status, error) {

        // toastr.error(request.responseText);
        clearInterval(intervalID);
      }
    });
  }, 300); // 1000 milliseconds = 1 second.
}

function fillSendMoneyStatusText(url, divId) {
  var url = localSparkService + url // the script where you handle the form input.
  var intervalID = setInterval(function() {
    $.ajax({
      type: "GET",
      url: url,
      xhrFields: {
        withCredentials: true
      },
      // data: seriesData, 
      success: function(data, status, xhr) {
        $(divId).html(data);
        if (data == "Success") {
          clearInterval(intervalID);
          $('.send-incomplete').fadeOut(1500);
          fillSimpleText('wallet/balance', '#balance');
        }
      },
      error: function(request, status, error) {

        // toastr.error(request.responseText);
        clearInterval(intervalID);
      }
    });
  }, 300); // 1000 milliseconds = 1 second.
}

function fillProgressBar(url, divId) {
  var url = localSparkService + url // the script where you handle the form input.
  var intervalID = setInterval(function() {
    $.ajax({
      type: "GET",
      url: url,
      xhrFields: {
        withCredentials: true
      },
      // data: seriesData, 
      success: function(data, status, xhr) {
        console.log(data);
        var pct = (data * 100.0) + "%";
        console.log(pct);
        $(divId).css({
          "width": pct
        });
        if (data == 1.0) {
          $(divId).removeClass('active progress-bar-striped');
          clearInterval(intervalID);

        }
      },
      error: function(request, status, error) {

        // toastr.error(request.responseText);
        clearInterval(intervalID);
      }
    });
  }, 100); // 1000 milliseconds = 1 second.
}

function standardFormPost(shortUrl, formId, modalId, reload, successFunctions, noToast, clearForm, sparkService, errorFunctions) {
  // !!!!!!They must have names unfortunately
  // An optional arg
  modalId = (typeof modalId === "undefined") ? "defaultValue" : modalId;

  reload = (typeof reload === "undefined") ? false : reload;

  noToast = (typeof noToast === "undefined") ? false : noToast;

  clearForm = (typeof clearForm === "undefined") ? false : clearForm;

  sparkService = (typeof sparkService === "undefined" || sparkService == null) ? localSparkService : sparkService;



  // serializes the form's elements.
  var formData = $(formId).serializeArray();
  // console.log(formData);

  var btn = $("[type=submit]", formId);

  // Loading
  btn.button('loading');

  url = sparkService + shortUrl;


  console.log(url);

  // console.log(url);
  $.ajax({
    type: "POST",
    url: url,
    xhrFields: {
      withCredentials: true
    },
    data: formData,
    success: function(data, status, xhr) {

      // console.log('posted the data');
      xhr.getResponseHeader('Set-Cookie');
      // document.cookie="authenticated_session_id=" + data + 
      // "; expires=" + expireTimeString(60*60); // 1 hour (field is in seconds)
      // Hide the modal, reset the form, show successful

      // $(formId)[0].reset();
      $(modalId).modal('hide');
      // console.log(modalId);
      if (!noToast) {
        toastr.success(data);
      }
      if (successFunctions != null) {
        successFunctions(data);
      }
      if (reload) {
        // refresh the page, too much info has now changed
        window.setTimeout(function() {
          location.reload();
        }, 3000);
      }

      btn.button('reset');
      if (clearForm) {
        $(formId)[0].reset();
      }
      // console.log(document.cookie);
      return data;

    },
    error: function(request, status, error) {
      if (errorFunctions != null) {
        errorFunctions(request);
      }

      if (request.responseText != null) {
        toastr.error(request.responseText);
      } else {
        toastr.error("Couldn't find endpoint " + url);

      }
      btn.button('reset');
    }
  });

  // if (event != null) {
  // event.preventDefault();
  // }
  return false;



  // event.preventDefault();
}

function simplePost(shortUrl, postData, reload, successFunctions, noToast, sparkService, btnId) {


  // !!!!!!They must have names unfortunately
  // An optional arg
  modalId = (typeof modalId === "undefined") ? "defaultValue" : modalId;

  reload = (typeof reload === "undefined") ? false : reload;

  noToast = (typeof noToast === "undefined") ? false : noToast;

  sparkService = (typeof sparkService === "undefined" || sparkService == null) ? localSparkService : sparkService;

  btnId = (typeof btnId === "undefined") ? false : btnId;

  var url;

  url = sparkService + shortUrl;


  // var btn = $("[type=submit]");
  // var btn = $(this).closest(".btn");
  var btn = $(btnId);

  // Loading
  btn.button('loading');

  // console.log(url);
  $.ajax({
    type: "POST",
    url: url,
    xhrFields: {
      withCredentials: true
    },
    data: postData,
    success: function(data, status, xhr) {

      // console.log('posted the data');
      xhr.getResponseHeader('Set-Cookie');
      // document.cookie="authenticated_session_id=" + data + 
      // "; expires=" + expireTimeString(60*60); // 1 hour (field is in seconds)
      // Hide the modal, reset the form, show successful

      // $(formId)[0].reset();

      // console.log(modalId);
      if (!noToast) {
        toastr.success(data);
      }
      if (successFunctions != null) {
        successFunctions(data);
      }
      btn.button('reset');
      if (reload) {
        // refresh the page, too much info has now changed
        window.setTimeout(function() {
          location.reload();
        }, 3000);
      }



      // console.log(document.cookie);
      return data;

    },
    error: function(request, status, error) {
      if (!noToast) {
        toastr.error(request.responseText);
      }
      btn.button('reset');
    }
  });


  return false;



  // event.preventDefault();


}

function setupPagedTable(shortUrl, templateHtml, divId, tableId) {
  var pageNum = pageNumbers[tableId];

  var nextId = divId + "_pager_next";
  var prevId = divId + "_pager_prev";
  // console.log(nextId);
  // TODO get page numbers here
  // fillTableFromMustache(shortUrl + pageNum,
  fillTableFromMustache(shortUrl,
    templateHtml, divId, tableId);

  $(nextId).click(function(e) {
    pageNum++;
    $(prevId).removeClass('disabled');

    fillTableFromMustache(shortUrl + pageNum,
      templateHtml, divId, tableId);

  });
  $(prevId).click(function(e) {
    if (pageNum > 1) {
      pageNum--;

      fillTableFromMustache(shortUrl + pageNum,
        templateHtml, divId, tableId);
    }
    if (pageNum == 1) {
      $(this).addClass('disabled');
      return;
    }


  });
}

function fillTableFromMustache(url, templateHtml, divId, tableId) {
  //         $.tablesorter.addParser({ 
  //           id: 'my_date_column', 
  //           is: function(s) { 
  //       // return false so this parser is not auto detected 
  //       return false; 
  //     }, 
  //     format: function(s) { 
  //       console.log(s);
  //       var timeInMillis = new Date.parse(s);

  //       // var date = new Date(parseInt(s));
  //       return date;         
  //     }, 
  //   // set type, either numeric or text 
  //   type: 'numeric' 
  // });

  var url = localSparkService + url // the script where you handle the form input.
  $.ajax({
    type: "GET",
    cache: true,
    url: url,
    xhrFields: {
      withCredentials: true
    },
    // data: seriesData, 
    success: function(data, status, xhr) {
      // console.log(data);
      if (data[0] == '[') {
        data = JSON.parse(data);
      }

      // JSON.useDateParser();
      // var jsonObj = jQuery.parseJSON(data);
      // JSON.useDateParser();
      // var jsonObj = JSON.parseWithDate(data);



      // console.log(data);
      //formatting the dates
      $.extend(data, standardDateFormatObj);




      Mustache.parse(templateHtml); // optional, speeds up future uses
      var rendered = Mustache.render(templateHtml, data);
      $(divId).html(rendered);
      $(tableId).tablesorter({
        debug: false
          // textExtraction: extractData
          //     headers: { 
          //   0: {       // Change this to your column position
          //     sorter:'my_date_column' 
          //   } 
          // }
      });
      // console.log(jsonObj);
      // console.log(templateHtml);
      // console.log(rendered);


    },
    error: function(request, status, error) {

      toastr.error(request.responseText);
    }
  });

}
// TODO
function fillMustacheFromJson(url, templateHtml, divId) {
  //         $.tablesorter.addParser({ 
  //           id: 'my_date_column', 
  //           is: function(s) { 
  //       // return false so this parser is not auto detected 
  //       return false; 
  //     }, 
  //     format: function(s) { 
  //       console.log(s);
  //       var timeInMillis = new Date.parse(s);

  //       // var date = new Date(parseInt(s));
  //       return date;         
  //     }, 
  //   // set type, either numeric or text 
  //   type: 'numeric' 
  // });

  var url = localSparkService + url // the script where you handle the form input.
  return $.ajax({
    type: "GET",
    url: url,
    cache: true,
    xhrFields: {
      withCredentials: true
    },
    // data: seriesData, 
    success: function(data, status, xhr) {
      // console.log(data);
      // var jsonObj = JSON.parse(data);
      // JSON.useDateParser();
      // var jsonObj = jQuery.parseJSON(data);
      // JSON.useDateParser();
      // var jsonObj = JSON.parseWithDate(data);

      $.extend(data, standardDateFormatObj);
      $.extend(data, otherDateFormatObj);
      $.extend(data, sparkServiceObj);
      $.extend(data, currencyFormatter);
      $.extend(data, htmlDecoder);
      $.extend(data, numToStars);
      $.extend(data, millisToMinutesAndSecondsObj);
      $.extend(data, seedersToTypeObj);
      Mustache.parse(templateHtml); // optional, speeds up future uses
      var rendered = Mustache.render(templateHtml, data);
      $(divId).html(rendered);

      console.log(data);
      // console.log(templateHtml);
      // console.log(rendered);


    },
    error: function(request, status, error) {
      toastr.error(request.responseText);
    }
  });

}

function fillMustacheWithJson(data, templateHtml, divId) {


  $.extend(data, standardDateFormatObj);
  $.extend(data, otherDateFormatObj);
  $.extend(data, sparkServiceObj);
  $.extend(data, currencyFormatter);
  $.extend(data, htmlDecoder);
  $.extend(data, numToStars);
  $.extend(data, millisToMinutesAndSecondsObj);
  $.extend(data, seedersToTypeObj);
  Mustache.parse(templateHtml); // optional, speeds up future uses
  var rendered = Mustache.render(templateHtml, data);
  $(divId).html(rendered);

  // console.log(rendered);

}

function mustacheFunctions(data) {
  $.extend(data, standardDateFormatObj);
  Mustache.parse(templateHtml); // optional, speeds up future uses
  var rendered = Mustache.render(templateHtml, data);
  $(divId).html(rendered);

  // console.log(jsonObj);
  // console.log(templateHtml);
  // console.log(rendered);
}


function getCookies() {
  var c = document.cookie,
    v = 0,
    cookies = {};
  if (document.cookie.match(/^\s*\$Version=(?:"1"|1);\s*(.*)/)) {
    c = RegExp.$1;
    v = 1;
  }
  if (v === 0) {
    c.split(/[,;]/).map(function(cookie) {
      var parts = cookie.split(/=/, 2),
        name = decodeURIComponent(parts[0].trimLeft()),
        value = parts.length > 1 ? decodeURIComponent(parts[1].trimRight()) : null;
      cookies[name] = value;
    });
  } else {
    c.match(/(?:^|\s+)([!#$%&'*+\-.0-9A-Z^`a-z|~]+)=([!#$%&'*+\-.0-9A-Z^`a-z|~]*|"(?:[\x20-\x7E\x80\xFF]|\\[\x00-\x7F])*")(?=\s*[,;]|$)/g).map(function($0, $1) {
      var name = $0,
        value = $1.charAt(0) === '"' ? $1.substr(1, -1).replace(/\\(.)/g, "$1") : $1;
      cookies[name] = value;
    });
  }
  return cookies;
}

function getCookie(c_name) {
  if (document.cookie.length > 0) {
    c_start = document.cookie.indexOf(c_name + "=");
    if (c_start != -1) {
      c_start = c_start + c_name.length + 1;
      c_end = document.cookie.indexOf(";", c_start);
      if (c_end == -1) {
        c_end = document.cookie.length;
      }
      return unescape(document.cookie.substring(c_start, c_end));
    }
  }
  return "";
}

function deleteCookie(name) {
  document.cookie = name + '=; path=/;expires=Thu, 01 Jan 1970 00:00:01 GMT;';
}

function createCookie(name, value, days) {
  if (days) {
    var date = new Date();
    date.setTime(date.getTime() + days * 24 * 60 * 60 * 1000); // ) removed
    var expires = "; expires=" + date.toGMTString(); // + added
  } else
    var expires = "";

  document.cookie = name + "=" + value + expires + ";path=/"; // + and " added
}

function countdown(divId, expireTimeInMS) {

  $(divId).countdown(expireTimeInMS, function(event) {
    $(this).text(
      event.strftime('%M:%S')
    );
  });

}

function formatMoney(num) {
  return num.toFixed(2).replace(/\d(?=(\d{3})+\.)/g, '$&,');
}

function getUrlParameter(sParam) {
  var sPageURL = window.location.search.substring(1);
  var sURLVariables = sPageURL.split('&');
  for (var i = 0; i < sURLVariables.length; i++) {
    var sParameterName = sURLVariables[i].split('=');
    if (sParameterName[0] == sParam) {
      return sParameterName[1];
    }
  }
}

function getUrlPathArray() {
  return window.location.pathname.split('/');
}

function getLastUrlPath() {
  var path;
  if (window.location.toString().startsWith('file:')) {
    path = getUrlPathArray().slice(-1)[0];
  } else {
    path = torrentTunesSparkService;
  }
  return path;

}

function insertParam(key, value) {
  key = escape(key);
  value = escape(value);

  var kvp = document.location.search.substr(1).split('&');
  if (kvp == '') {
    document.location.search = '?' + key + '=' + value;
  } else {

    var i = kvp.length;
    var x;
    while (i--) {
      x = kvp[i].split('=');

      if (x[0] == key) {
        x[1] = value;
        kvp[i] = x.join('=');
        break;
      }
    }

    if (i < 0) {
      kvp[kvp.length] = [key, value].join('=');
    }
    var newURL = kvp.join('&');
    console.log(newURL);
    // console.log(getLastUrlPath());

    window.history.pushState({
      path: newURL
    }, 'TorrentTunes', getLastUrlPath() + '?' + newURL);
  }

}

function replaceParams(key, value) {
  var newURL = '?' + key + '=' + value;
  window.history.pushState({
    path: newURL
  }, 'TorrentTunes', getLastUrlPath() + newURL);
}

function clearParams() {
  var url = getLastUrlPath();
  window.history.pushState({
    path: url
  }, 'TorrentTunes', url);
  // window.history.go(-window.history.length+1);
  // window.history.go(window.history[0]);
}




var standardDateFormatObj = {
  "dateformat": function() {
    return function(text, render) {
      var t = render(text);
      var date = new Date(parseInt(t));
      // console.log(t);
      return date.customFormat("#YYYY#/#MM#/#DD# #hh#:#mm# #AMPM#")
    }
  }
};

var millisToMinutesAndSecondsObj = {
  "millisToMinutesAndSeconds": function() {
    return function(text, render) {
      var t = render(text);
      var millis = parseInt(t);
      return millisToMinutesAndSeconds(millis);
    }
  }
}

var seedersToTypeObj = {
  "seedersToType": function() {
    return function(text, render) {
      var t = render(text);

      // don't ask me why this logic works, trial and error
      if (!isNaN(t)) {
        return "muted";
      }

      var peerInfo = t.split("-").map(Number);
      var peers = peerInfo.max();
      // console.log('peers = ' + peers);

      var type;
      if (peers >= 3) {
        type = "success";
      } else if (peers >= 1) {
        type = "warning";
      } else if (peers == 0) {
        type = "danger";
      }

      return type;
    }
  },
  "seedersToNum": function() {
    return function(text, render) {
      var t = render(text);
      if (!isNaN(t)) {
        return "Unknown # of";
      }

      var peerInfo = t.split("-").map(Number);
      var peers = peerInfo.max();

      return peers;

    }
  }
}

function millisToMinutesAndSeconds(millis) {
  var minutes = Math.floor(millis / 60000);
  var seconds = ((millis % 60000) / 1000).toFixed(0);
  return minutes + ":" + (seconds < 10 ? '0' : '') + seconds;
}

var otherDateFormatObj = {
  "otherdateformat": function() {
    return function(text, render) {
      var t = render(text);
      var timeStrArr = t.split(/[-: ]/);
      var a = timeStrArr.map(function toInt(x) {
        return parseInt(x);
      });
      // months are off by 1 in js, and hours are off by the timezone offset
      var tzOffset = new Date().getTimezoneOffset() / 60;
      var date = new Date(a[0], a[1] - 1, a[2], a[3] - tzOffset, a[4], a[5]);
      // console.log(a);
      // console.log(date);
      return date.customFormat("#YYYY#/#MM#/#DD# #hh#:#mm# #AMPM#")
    }
  }
};

var currencyFormatter = {
  "toFixed": function() {
    return function(num, render) {
      return parseFloat(render(num)).toFixed(2).replace(/\d(?=(\d{3})+\.)/g, '$&,');
    }
  }
};

var htmlDecoder = {
  "htmlDecode": function() {
    return function(text, render) {
      var t = render(text).replace(/semicolon/g, ';');
      console.log(t);
      return $('<div/>').html(t).text();
    }
  }
};

var numToStars = {
  "numToStars": function() {
    return function(text, render) {
      var t = render(text);
      var num = parseFloat(t);
      var num2 = parseInt(Math.floor(num));
      var frac = (num2 % 2);
      var starText = "";

      for (var i = 0; i < num2; i++) {
        starText += '<i class="fa fa-star"></i>';
      }

      if (frac > 0) {
        starText += '<i class="fa fa-star-half-o"></i>';
      }

      return starText;
    }
  }
};


Date.prototype.customFormat = function(formatString) {
  var YYYY, YY, MMMM, MMM, MM, M, DDDD, DDD, DD, D, hhh, hh, h, mm, m, ss, s, ampm, AMPM, dMod, th;
  var dateObject = this;
  YY = ((YYYY = dateObject.getFullYear()) + "").slice(-2);
  MM = (M = dateObject.getMonth() + 1) < 10 ? ('0' + M) : M;
  MMM = (MMMM = ["January", "February", "March", "April", "May", "June", "July", "August", "September", "October", "November", "December"][M - 1]).substring(0, 3);
  DD = (D = dateObject.getDate()) < 10 ? ('0' + D) : D;
  DDD = (DDDD = ["Sunday", "Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday"][dateObject.getDay()]).substring(0, 3);
  th = (D >= 10 && D <= 20) ? 'th' : ((dMod = D % 10) == 1) ? 'st' : (dMod == 2) ? 'nd' : (dMod == 3) ? 'rd' : 'th';
  formatString = formatString.replace("#YYYY#", YYYY).replace("#YY#", YY).replace("#MMMM#", MMMM).replace("#MMM#", MMM).replace("#MM#", MM).replace("#M#", M).replace("#DDDD#", DDDD).replace("#DDD#", DDD).replace("#DD#", DD).replace("#D#", D).replace("#th#", th);

  h = (hhh = dateObject.getHours());
  if (h == 0) h = 24;
  if (h > 12) h -= 12;
  hh = h < 10 ? ('0' + h) : h;
  AMPM = (ampm = hhh < 12 ? 'am' : 'pm').toUpperCase();
  mm = (m = dateObject.getMinutes()) < 10 ? ('0' + m) : m;
  ss = (s = dateObject.getSeconds()) < 10 ? ('0' + s) : s;
  return formatString.replace("#hhh#", hhh).replace("#hh#", hh).replace("#h#", h).replace("#mm#", mm).replace("#m#", m).replace("#ss#", ss).replace("#s#", s).replace("#ampm#", ampm).replace("#AMPM#", AMPM);
}




var delay = (function() {
  var timer = 0;
  return function(callback, ms) {
    clearTimeout(timer);
    timer = setTimeout(callback, ms);
  };
})();

function htmlDecode(value) {
  return $('<div/>').html(value.replace(/semicolon/g, ';')).text();
}

function htmlEncode(value) {
  return $('<div/>').text(value).html();
}

if (typeof String.prototype.startsWith != 'function') {
  String.prototype.startsWith = function(str) {
    return this.slice(0, str.length) == str;
  };
}

function loggedIn() {
  var sessionId = getCookie("authenticated_session_id");

  var ret = (sessionId != null) ? true : false;

  return ret;

}



function voteBtn() {
  $('.vote-btn').click(function(e) {
    e.preventDefault();
    var id = '#' + this.id;
    console.log("id = " + id);

    // Voting on reviews
    if (id.startsWith("#review_vote")) {
      //review_vote_helpful_number
      //review_vote_not_helpful_number
      var reviewId = id.split('_').pop();
      console.log('review id = ' + reviewId);


      if (id.startsWith('#review_vote_helpful_')) {
        // Get the opposite one to remove a potential success class from it:

        var voteUrl = 'review_vote/' + reviewId + '/up';
        simplePost(voteUrl, null, null, (function() {

          var downVoteId = '#review_vote_not_helpful_' + reviewId;
          $(id).addClass('btn-success');
          $(downVoteId).removeClass('btn-danger');
        }), null, null, id);


      } else {
        // Get the opposite one to remove a potential success class from it:
        var voteUrl = 'review_vote/' + reviewId + '/down';

        simplePost(voteUrl, null, null, (function() {
          var upVoteId = '#review_vote_helpful_' + reviewId;
          $(id).addClass('btn-danger');
          $(upVoteId).removeClass('btn-success');
        }), null, null, id);

      }
    }

    // Voting on questions
    else if (id.startsWith("#question_vote")) {

      var questionId = id.split('_').pop();
      console.log('question id = ' + questionId);

      var voteNumber = parseInt($('#question_vote_number_' + questionId).text());

      if (id.startsWith('#question_vote_up_')) {
        // Get the opposite one to remove a potential success class from it:

        var voteUrl = 'question_vote/' + questionId + '/up';
        simplePost(voteUrl, null, null, (function() {

          var downVoteId = '#question_vote_down_' + questionId;
          $(id).addClass('btn-success');
          $(downVoteId).removeClass('btn-danger');
          $('#question_vote_number_' + questionId).text(++voteNumber);

        }), null, null, id);


      } else {
        // Get the opposite one to remove a potential success class from it:
        var voteUrl = 'question_vote/' + questionId + '/down';

        simplePost(voteUrl, null, null, (function() {
          var upVoteId = '#question_vote_up_' + questionId;
          $(id).addClass('btn-danger');
          $(upVoteId).removeClass('btn-success');
          $('#question_vote_number_' + questionId).text(--voteNumber);
        }), null, null, id);

      }
    }
    // Voting on answers
    else if (id.startsWith("#answer_vote")) {

      var answerId = id.split('_').pop();
      console.log('answer id = ' + answerId);

      var voteNumber = parseInt($('#answer_vote_number_' + answerId).text());

      if (id.startsWith('#answer_vote_up_')) {
        // Get the opposite one to remove a potential success class from it:

        var voteUrl = 'answer_vote/' + answerId + '/up';
        simplePost(voteUrl, null, null, (function() {

          var downVoteId = '#answer_vote_down_' + answerId;
          $(id).addClass('btn-success');
          $(downVoteId).removeClass('btn-danger');
          $('#answer_vote_number_' + answerId).text(++voteNumber);

        }), null, null);


      } else {
        // Get the opposite one to remove a potential success class from it:
        var voteUrl = 'answer_vote/' + answerId + '/down';

        simplePost(voteUrl, null, null, (function() {
          var upVoteId = '#answer_vote_up_' + answerId;
          $(id).addClass('btn-danger');
          $(upVoteId).removeClass('btn-success');
          $('#answer_vote_number_' + answerId).text(--voteNumber);
        }), null, null);

      }


    }



  });
}

function replyBtn() {


  $('.reply-btn').click(function(e) {
    e.preventDefault();
    var id = '#' + this.id;
    console.log("id = " + id);

    var questionId = id.split('_').pop();

    // unhide the reply form
    var formName = '#question_reply_form_' + questionId;
    $(formName).removeClass('hide');

    $(formName).bootstrapValidator({
        message: 'This value is not valid',
        excluded: [':disabled'],
        submitButtons: 'button[type="submit"]'
      })
      .on('success.form.bv', function(event) {
        event.preventDefault();

        standardFormPost('answer_question/' + questionId, formName, null, null, null, null, null);

      });

  });
}

function fillReviewVotes(reviews) {

  reviews.forEach(function(e) {
    var reviewId = e['id'];

    console.log('review id = ' + reviewId);

    // get the users vote on that review
    var url = 'get_review_vote/' + reviewId;

    getJson(url).done(function(e1) {
      var reviewVote = JSON.parse(e1);
      console.log(reviewVote);
      var voteInt = (reviewVote['vote'] == 'up') ? 1 : 0;

      console.log(voteInt);
      if (voteInt == 1) {
        $('#review_vote_helpful_' + reviewId).addClass('btn-success');
      } else {
        $('#review_vote_not_helpful_' + reviewId).addClass('btn-danger');
      }

    });


  });

}

function fillQuestionVotes(questions) {

  questions.forEach(function(e) {
    var questionId = e['id'];

    // get the users vote on that review
    var url = 'get_question_vote/' + questionId;

    getJson(url).done(function(e1) {
      if (e1 != '[]') {
        var questionVote = JSON.parse(e1);
        var voteInt = (questionVote['vote'] == 'up') ? 1 : 0;
        console.log(questionVote);

        console.log(voteInt);
        if (voteInt == 1) {
          $('#question_vote_up_' + questionId).addClass('btn-success');
        } else {
          $('#question_vote_down_' + questionId).addClass('btn-danger');
        }
      }
    });


  });

}

function fillAnswerVotes(answers) {

  answers.forEach(function(e) {
    var answerId = e['id'];

    // get the users vote on that review
    var url = 'get_answer_vote/' + answerId;

    getJson(url).done(function(e1) {
      if (e1 != '[]') {
        var answerVote = JSON.parse(e1);
        var voteInt = (answerVote['vote'] == 'up') ? 1 : 0;
        console.log(answerVote);

        console.log(voteInt);
        if (voteInt == 1) {
          $('#answer_vote_up_' + answerId).addClass('btn-success');
        } else {
          $('#answer_vote_down_' + answerId).addClass('btn-danger');
        }
      }
    });


  });

}

function tabLoad() {
  // Javascript to enable link to tab
  var hash = document.location.hash;
  var prefix = "tab_";
  if (hash) {
    console.log('hash');
    $('.nav-tabs a[href=' + hash.replace(prefix, "") + ']').tab('show');
    $('#rootwizard').find("a[href*='" + hash + "']").trigger('click');
  }

  // Change hash for page-reload
  $('.nav-tabs a').on('shown.bs.tab', function(e) {
    window.location.hash = e.target.hash.replace("#", "#" + prefix);
    $('#rootwizard').find("a[href*='" + hash + "']").trigger('click');
  });
}

Array.prototype.contains = function(obj) {
  var i = this.length;
  while (i--) {
    if (this[i] === obj) {
      return true;
    }
  }
  return false;
}


Array.prototype.max = function() {
  return Math.max.apply(null, this);
};

Array.prototype.min = function() {
  return Math.min.apply(null, this);
};

function isMobile() {
  return /Android|webOS|iPhone|iPad|iPod|BlackBerry|BB|PlayBook|IEMobile|Windows Phone|Kindle|Silk|Opera Mini/i.test(navigator.userAgent);
}

function findIndexInArray(array, key, value) {
  for (var i = 0; i < array.length; i++) {
    if (array[i][key] === value) {
      return i;
    }
  }
  return null;
}

function hideKeyboard(element) {
    element.attr('readonly', 'readonly'); // Force keyboard to hide on input field.
    element.attr('disabled', 'true'); // Force keyboard to hide on textarea field.
    setTimeout(function() {
        element.blur();  //actually close the keyboard
        // Remove readonly attribute after keyboard is hidden.
        element.removeAttr('readonly');
        element.removeAttr('disabled');
    }, 100);
}
