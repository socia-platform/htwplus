$(window).resize(function() {
	if ((800 / 1200 * $('div#login').innerWidth()) < $('div#login').innerHeight()) {
		$('div#login').css('background-size', 'auto 100%');
	} else {
		$('div#login').css('background-size', '100% auto');
	}
});