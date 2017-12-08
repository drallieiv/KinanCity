var maxTime = 120;
var domain = "http://localhost:8888/"
var dataSiteKey = "6LdpuiYTAAAAAL6y9JNUZzJ7cF3F8MQGGKko1bCy";

var style = '<style>html, body{width: 100%;height: 100%;margin: 0px;padding: 0px;position: relative;} .centered-block {max-height: 50%;margin-left: auto;margin-right: auto;text-align: center;} .g-recaptcha div{margin-left: auto;margin-right: auto; text-align: center;} .img-responsive{max-width: 90%;height: 100%;} #timer,#status{text-align: center;padding-bottom: 4px;} #messages{width: 180px;font-size: 14px;margin: 0 auto;padding: 4px;border: 1px solid grey;border-radius: 4px;background: ghostwhite;font-family: sans-serif;} .label{width: 140px;text-align: right;padding-right: 2px;display: inline-block;}</style>';
var statsElement = '<div id="messages"><div id="timer"></div><div id="status"></div><span class="label">Jobs Pending:</span><strong id="jobs_pending">0</strong></div>';
var captchaOnly = '<div id="recaptcha"><div class="g-recaptcha" data-size="compact" data-sitekey="'+dataSiteKey+'" data-callback="captchaResponse"></div></div>';
var captchaPage = '<html>\n  <head>\n    <title>Kinan Server Captchas</title>\n    <meta name="viewport" content="width=device-width, initial-scale=.9"/>\n     ' + style + '</head>\n  <body>\n    <div class="content">\n      <form action="?" method="POST">\n        ' + captchaOnly + '\n      </form>\n<br /><br />' + statsElement + '    </div>\n  </body>\n</html>';
var last_res = null;

var timer = maxTime;
var timer_interval;

var running = false;

function initCaptchaPage(){
	document.body.parentElement.innerHTML = captchaPage;
	
    var script = document.createElement('script');
    script.src = 'https://cdnjs.cloudflare.com/ajax/libs/jquery/3.1.0/jquery.min.js';
    script.type = 'text/javascript';
    document.getElementsByTagName('head')[0].appendChild(script);
	    
	if(running){
	
	    script = document.createElement('script');
	    script.src = 'https://www.google.com/recaptcha/api.js';
	    script.type = 'text/javascript';
	    document.getElementById('recaptcha').appendChild(script);
	
	    clearInterval(timer_interval);
	    setTimeout(refreshStats, 1000);
	    timer = maxTime;
	    timer_interval = setInterval(function() {
	        timer--;
	        if(timer < 0) {
	            $('#timer').text('Refreshing captcha...');
	            setTimeout(initCaptchaPage, 1000);
	        } else {
	            $('#timer').html('<strong>' + timer.toString() + '</strong> seconds until refresh.');
	        }
	    }, 1000);
    }else{
    	var msg = document.createElement('p');
    	msg.append("Waiting for jobs");
    	document.getElementById('messages').appendChild(msg);
    	setTimeout(refreshStats, 200);
    }
}

function refreshStats() {
    $.getJSON(domain + '/captcha/jobs/stats', function(data){
        $('#jobs_pending').text(data.jobsPending);
        if(!running){
        	if(data.jobsPending == 0){
        		setTimeout(refreshStats, 5000);
        	}else{
        		running = true;
        		setTimeout(initCaptchaPage, 1000);
        	}
    	}else{
    		if(data.jobsPending == 0){
        		running = false;
        	}
    	}
    });
}

var fnc = function(str){
    var elem = document.getElementById('g-recaptcha-response');
    var res  = elem ? (elem.value || str) : str;

    setTimeout(function(){
        if(res && last_res !== res){
            console.log(res);
            last_res = res;
            $.ajax({
        		type: "post",
        		url: domain + 'captcha/solve',
        		dataType:"text",
        		data: {token: res},
        		success: function(){
        			$('#status').text('Captcha token submitted.');
        		},
        		error: function(jqXHR, status, err){
        			$('#status').text('Failed to submit captcha token.');
        		},
        		complete: function(){
        			$('#status').fadeIn(200);
        		}
        	});
            setTimeout(initCaptchaPage, 1500);
        }
    }, 1);
};

captchaResponse=fnc;
setInterval(fnc, 500);
initCaptchaPage();
