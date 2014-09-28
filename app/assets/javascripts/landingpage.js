var imgHeight;
var imgWidth;

function resizeBackground() {
	if ((imgHeight / imgWidth * $('div#hp-login').innerWidth()) < $('div#hp-login').innerHeight()) {
		$('div#hp-login').css('background-size', 'auto 100%');
	} else {
		$('div#hp-login').css('background-size', '100% auto');
	}
}

$(window).load(function() {
    var newImg = new Image();
    newImg.src = 'http://www.htw-berlin.de/fileadmin/HTW/Zentral/DE/HTW/ZR1_Presse/Pressefotos/130719___Philipp_Meuser_0011_01_1200px_crop.jpg';
    imgHeight = newImg.height;
    imgWidth = newImg.width;
    resizeBackground();
});

$(window).resize(resizeBackground);

$('#hp-navbar').affix({
    offset: {
        top: function () { return $('#hp-login').outerHeight(); }
    }
})