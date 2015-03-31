$(document).ready(function () {
    'use strict';
    var console = window.console || { log: function () {} };

    function AvatarUpload(element) {
        this.$element = $(element);
        this.$uploadForm = this.$element.find('#hp-avatar-uploadform');
        this.$uploadInput = this.$element.find('.hp-avatar-fileinput')[0];
        this.$finishForm = this.$element.find('#hp-avatar-upload-finish');
        this.$uploadButton = this.$element.find('input');
        this.$errorMessage = this.$element.find('.hp-avatar-flash');
        this.$loading = this.$element.find('.hp-avatar-loading');
        this.$modal = this.$element.find('.modal');
        this.$previewImg = this.$element.find('#hp-avatar-preview');
        this.$saveButton = this.$element.find('#hp-avatar-save-button');
        this.$profileAvatar = this.$element.find('.hp-avatar-medium');
        this.cropBoxData = {};
        this.init();
    }

    AvatarUpload.prototype = {

        init: function () {

            var _this = this;
            this.$uploadButton.change(function (e) {
                _this.uploadFile();
            });
            this.$modal.on('shown.bs.modal', function () {
                _this.$previewImg.cropper({
                    zoomable: false,
                    aspectRatio: 1/1,
                    background: false,
                    guides: false,
                    built: function () {
                        _this.$previewImg.cropper('setCropBoxData', _this.cropBoxData);
                    }
                });
            });
            this.$modal.on('hide.bs.modal', function () {
                _this.$previewImg.cropper('destroy');
            });
            this.$saveButton.click(function () {
                _this.cropSuccess();
            });
        },

        uploadFile: function () {
            this.resetError();
            if(!this.validateFile()){
                return;
            }
            this.showLoading();
            var data = new FormData(this.$uploadForm[0]);
            var url = this.$uploadForm.attr('action');
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

        validateFile: function () {
            var allowedTypes = ['image/jpeg', 'image/png'];
            var file = this.$uploadInput.files[0];
            var type = file.type;
            var size = file.size;
            size = size / 1024 / 1024;
            if(allowedTypes.indexOf(type) == -1){
                this.showError("Das Dateiformat wird nicht unterstützt.");
                return false;
            }
            if(size > 3){
                this.showError("Das Bild ist leider zu groß.");
                return false;
            }
            return true;
        },

        uploadError: function (XMLHttpRequest, textStatus, errorThrown) {
            var error = XMLHttpRequest.responseJSON.error;
            this.$errorMessage.html(error);
            this.$errorMessage.addClass("hp-avatar-error");
            this.hideLoading();
        },
        
        uploadSuccess: function (data) {
            var _this = this;
            var img =  this.$previewImg.attr("src");
            var d = new Date();
            this.$previewImg.attr("src", img+"?"+d.getTime());
            this.$previewImg.load(function () {
                _this.hideLoading();
                _this.$modal.modal();
            });
        },

        showError: function (message) {
            this.$errorMessage.html(message);
            this.$errorMessage.addClass("hp-avatar-error");
        },

        resetError: function () {
            this.$errorMessage.html("Max. 3 MB");
            this.$errorMessage.removeClass("hp-avatar-error");
        },
        
        cropSuccess: function () {
            var _this = this;
            var data = this.$previewImg.cropper('getData');
            var payload = {
                x: Math.round(data.x),
                y: Math.round(data.y),
                width: Math.round(data.width),
                height: Math.round(data.width)
            };
            console.log(data);
            var url = this.$finishForm.attr('action');
            $.ajax(url, {
                type: 'POST',
                contentType: "application/json",
                data: JSON.stringify(payload),
                dataType: 'json',
                processData: false,
                success: function () {
                    _this.$modal.hide();
                    location.reload();
                },
                error: function (XMLHttpRequest, textStatus, errorThrown) {
                    _this.$modal.hide();
                    _this.uploadError(XMLHttpRequest, textStatus, errorThrown);
                }
            });
        },

        showLoading: function () {
            this.$loading.css("display", "inline-block");
        },

        hideLoading: function () {
            this.$loading.css("display", "none");
        }
    };

    new AvatarUpload("#hp-avatar-upload");

});