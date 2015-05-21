/**
 * jQuery Auto Pagination v1.0
 * Copyright 2013 Choy Peng Kong
 * An unobstrusive auto pagination plugin for JQuery
 *
 * Inspired by "Infinite Scroll" JQuery Plugin
 * by Paul Irish & Luke Shumard
 *
 * Dual licensed under the MIT and GPL
 *   http://www.opensource.org/licenses/mit-license.php
 *   http://www.gnu.org/licenses/gpl.html
 */
(function( $ ) {
    $.fn.AutoPagination = function( options ) {

        var opts = $.extend( {}, $.fn.AutoPagination.defaults, options ),
            $this = $( this );

        // Hide the 'nextPageSelector' anchor since it the point of this
        // plugin is that one shouldn't need to click 'Next Page'
        $( opts.nextPageSelector ).css( { visibility:'hidden' } );

        // scroll event fires repeatedly as the window is scrolled
        $( window ).scroll(function() {

            // if 'nextPageSelector' anchor href isn't empty and...
            if ( $( opts.nextPageSelector ).attr( 'href' ) &&
                    // ...window scroll is less then 'nextPageBufferPx' pixels away from it
                $( window ).scrollTop() + $( window ).height() >
                $( opts.nextPageSelector ).offset().top - opts.nextPageBufferPx ) {

                // remember the 'nextPageSelector' anchor href as 'nextPage'
                var nextPage = $( opts.nextPageSelector ).attr( 'href' );

                // set the 'nextPageSelector' anchor href to an empty string
                $( opts.nextPageSelector ).attr( 'href', '' );

                // add loader div to DOM after last panel
                $( '<div class="'+opts.loaderDivClass+'" style="'+opts.loaderDivStyle+'"><span>'+
                (opts.loaderImage ? '<img src="'+opts.loaderImage+'" alt="'+opts.loaderText+'" />' : opts.loaderText)+
                '</span></div>' ).insertAfter( $this.find( opts.panelSelector ).last() );

                // use ajax 'GET' to grab the 'nextPage' html
                $.get( nextPage, function(data) {

                    // for each 'panelSelector' element in the loaded data...
                    $( data ).find( opts.panelSelector ).each(function( index, el ) {
                        // ...insert it after the last 'panelSelector'
                        $( el ).insertAfter( $this.find( opts.panelSelector ).last() );
                    });

                    // remove loader div from DOM
                    $( '.'+opts.loaderDivClass ).hide();

                    // reset the 'nextPageSelector' anchor href
                    $( opts.nextPageSelector ).attr( 'href', $( data ).find( opts.nextPageSelector ).attr( 'href' ) );

                }); // closing of ajax 'GET'

            } // closing of if 'nextPageSelector' anchor...

        }); // closing of scroll event
    };

    $.fn.AutoPagination.defaults = {
        nextPageBufferPx: 200,
        nextPageSelector: '.nextPage',
        panelSelector: '.panel',
        loaderDivClass: 'ajax-loader',
        loaderDivStyle: 'text-align:center',
        loaderImage: 'ajax-loader.gif',
        loaderText: 'Loading...'
    };
})(jQuery);