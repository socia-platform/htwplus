$(document).ready(function () {

    var avatarDropzone = new Dropzone("#hp-avatar-upload", {
        paramName: "avatarimage",
        maxFiles: 1,
        maxFilesize: 20,
        createImageThumbnails: false,
        acceptedFiles: ".jpg",
        
    });
    avatarDropzone.on("complete", function(file) {
        avatarDropzone.removeFile(file);
    });
    avatarDropzone.on("error", function(file) {
        //alert("hallo");
    });

    var $image = $('#cropper-example-2 > img'),
        canvasData,
        cropBoxData;

    $('#cropper-example-2-modal').on('shown.bs.modal', function () {
        $image.cropper({
            zoomable: false,
            aspectRatio: 1/1,
            built: function () {
                $image.cropper('setCanvasData', canvasData);
                $image.cropper('setCropBoxData', cropBoxData);
            }
        });
    }).on('hidden.bs.modal', function () {
        canvasData = $image.cropper('getCanvasData');
        cropBoxData = $image.cropper('getCropBoxData');
        $.post( "ajax/test.html", function( data ) {
            $( ".result" ).html( data );
        });
        $image.cropper('destroy');
    });
});