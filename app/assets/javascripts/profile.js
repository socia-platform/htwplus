
$(document).ready(function () {
    'use strict';
    var console = window.console || { log: function () {} };

    function AvatarUpload(element) {
        this.$element = $(element);
        this.$uploadButton = this.$element.find('input');
        this.$errorMessage = this.$element.find('.hp-avatar-flash');
        this.$loading = this.$element.find('.loading');
        this.init();
    }

    AvatarUpload.prototype = {

        init: function () {
            var _this = this;
            this.$uploadButton.change(function (e) {
                _this.uploadFile();
            });
        },

        uploadFile: function () {
            //this.$loading.show();
            var data = new FormData(this.$element[0]);
            var url = this.$element.attr('action');
            var _this = this;
            $.ajax(url, {
                type: 'POST',
                data: data,
                dataType: 'json',
                processData: false,
                contentType: false,
                error: function (XMLHttpRequest, textStatus, errorThrown) {
                    _this.uploadError(XMLHttpRequest, textStatus, errorThrown);
                },
                success: function (data) {
                    _this.uploadSuccess(data);
                }
            });
        },

        uploadError: function (XMLHttpRequest, textStatus, errorThrown) {
            var error = XMLHttpRequest.responseJSON.error;
            this.$errorMessage.html(error);
            this.$errorMessage.addClass("hp-avatar-error");
        },
        
        uploadSuccess: function (data) {
            this.$loading.show();
            this.$loading.css('display', 'inline-block');
            var imgSelector = $('#cropper-example-2 > img');
            var img =  imgSelector.attr("src");
            var d = new Date();
            imgSelector.attr("src", img+"?"+d.getTime());
            imgSelector.load(function (e) {
                $('#cropper-example-2-modal').modal();
            });
            this.$loading.hide();
        }
        

    };

    new AvatarUpload("#hp-avatar-upload");


    //var avatarDropzone = new Dropzone("#hp-avatar-upload", {
    //    paramName: "avatarimage",
    //    maxFiles: 1,
    //    maxFilesize: 20,
    //    createImageThumbnails: false,
    //    acceptedFiles: ".jpg"
    //
    //});
    //avatarDropzone.on("success", function(file, response) {
    //    var imgSelector = $('#cropper-example-2 > img');
    //    var img =  imgSelector.attr("src");
    //    var d = new Date();
    //    imgSelector.attr("src", img+"?"+d.getTime());
    //    avatarDropzone.removeFile(file);
    //    $('#cropper-example-2-modal').modal();
    //});
    //avatarDropzone.on("error", function(file) {
    //    //alert("hallo");
    //});

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
            },
            crop: function (data) {
                $('#hp-avatar-upload-finish > input[name=x]').val(Math.round(data.x));
                $('#hp-avatar-upload-finish > input[name=y]').val(Math.round(data.y));
                $('#hp-avatar-upload-finish > input[name=height]').val(Math.round(data.height));
                $('#hp-avatar-upload-finish > input[name=width]').val(Math.round(data.width));
            }
        });
    }).on('hidden.bs.modal', function () {
        canvasData = $image.cropper('getCanvasData');
        cropBoxData = $image.cropper('getCropBoxData');
        $image.cropper('destroy');
    });

    $('#hp-avatar-save-button').click(function () {
        $('#cropper-example-2-modal').modal('hide');
        canvasData = $image.cropper('getCanvasData');
        cropBoxData = $image.cropper('getCropBoxData');
        $image.cropper('destroy');
        //$.post( "ajax/test.html", function( data ) {
        //    $( ".result" ).html( data );
        //});

    });

});