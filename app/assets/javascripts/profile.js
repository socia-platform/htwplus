$(document).ready(function () {
    $('#hp-avatar-upload').fileupload({
        dataType: 'json',
        add: function(e, data) {
                var uploadErrors = [];
                var acceptFileTypes = /^image\/(gif|jpe?g|png)$/i;
                if(data.originalFiles[0]['type'].length && !acceptFileTypes.test(data.originalFiles[0]['type'])) {
                    uploadErrors.push('Not an accepted file type');
                }
                if(data.originalFiles[0]['size'] > 50000000) {
                    uploadErrors.push('Filesize is too big');
                }
                if(uploadErrors.length > 0) {
                    alert(uploadErrors.join("\n"));
                } else {
                    data.submit();
                }
        },
        autoUpload: false,
        progressall: function (e, data) {
            var progress = parseInt(data.loaded / data.total * 100, 10);
            $('#hp-avatar-upload-progress').html(progress + '%');
        },
        done: function (e, data) {
            //$('#hp-avatar-upload-progress').html("Done");
        }
    });
});