module.exports = function(grunt) {

    var js_src = ['public/javascripts/**/*.js', 'public/javascripts/*.js'];
    var css_src = ['public/stylesheets/**/*.css', 'public/stylesheets/*.css'];
    var js_dest = 'public/javascripts/';
    var css_dest = 'public/stylesheets/';

    grunt.initConfig({
        pkg: grunt.file.readJSON('package.json'),

        jshint: {
            files: [js_src, 'Gruntfile.js'],
            options: {
                browser: true,
                jquery: true,
                globals: {
                    console: true,
                    module: true
                }
            }
        },

        concat: {
            mainPage_js: {
                src: [js_src, '!public/javascripts/landingpage.js'],
                dest: js_dest + '<%= pkg.name %>_mainpage.js'
            },
            landingPage_js: {
                src: [js_src, '!public/javascripts/main.js', '!public/javascripts/notification.js'],
                dest: js_dest + '<%= pkg.name %>_landingpage.js'
            },
            css: {
                src: css_src,
                dest: css_dest + '<%= pkg.name %>.css'
            }
        },

        bower_concat: {
            mainPage: {
                dest: js_dest + 'mainPageDependencies.js',
                cssDest: css_dest + 'mainPageDependencies.css',
                exclude: [
                    'jquery-scrollspy',
                    'ScrollMagic',
                    'gsap'
                ]
            },
            landingPage: {
                dest: js_dest + 'landingPageDependencies.js',
                cssDest: css_dest + 'landingPageDependencies.css',
                exclude: [
                    'handlebars',
                    'typehead.js',
                    'jQuery-linkify'
                ]
            }
        },

        uglify: {
            options: {
                banner: ''
            },
            all: {
                files: {
                    '<%= concat.mainPage_js.dest %>': ['<%= concat.mainPage_js.dest %>'],
                    '<%= concat.landingPage_js.dest %>': ['<%= concat.landingPage_js.dest %>'],
                    '<%= bower_concat.mainPage.dest %>': ['<%= bower_concat.mainPage.dest %>'],
                    '<%= bower_concat.landingPage.dest %>': ['<%= bower_concat.landingPage.dest %>']
                }
            }
        },

        cssmin: {
            all: {
                files: {
                    '<%= concat.css.dest %>': ['<%= concat.css.dest %>'],
                    '<%= bower_concat.mainPage.cssDest %>': ['<%= bower_concat.mainPage.cssDest %>'],
                    '<%= bower_concat.landingPage.cssDest %>': ['<%= bower_concat.landingPage.cssDest %>']
                }
            }
        },

        injector: {
            dev: {
                files: {
                    'app/views/main.scala.html': ['bower.json', '<%= concat.mainPage_js.src %>', '<%= concat.css.src %>'],
                    'app/views/landingpage.scala.html': ['bower.json', '<%= concat.landingPage_js.src %>', '<%= concat.css.src %>']
                }
            },
            prod: {
                files: {
                    'app/views/main.scala.html': ['<%= bower_concat.mainPage.dest %>', '<%= concat.mainPage_js.dest %>',
                        '<%= bower_concat.mainPage.cssDest %>', '<%= concat.css.dest %>'],
                    'app/views/landingpage.scala.html': ['<%= bower_concat.landingPage.dest %>', '<%= concat.landingPage_js.dest %>',
                        '<%= bower_concat.landingPage.cssDest %>', '<%= concat.css.dest %>']
                }
            }
        },

        clean: {
            js: ['<%= bower_concat.mainPage.dest %>', '<%= bower_concat.landingPage.dest %>',
                '<%= concat.mainPage_js.dest %>', '<%= concat.landingPage_js.dest %>'],
            css: ['<%= bower_concat.mainPage.cssDest %>', '<%= bower_concat.landingPage.cssDest %>',
                '<%= concat.css.dest %>']
        },

        watch: {
            files: ['<%= jshint.files %>'],
            tasks: ['jshint']
        },

        exec: {
            twbs_compile: {
                command: 'npm install && grunt dist',
                cwd: 'public/bower_components/bootstrap'
            }
        },

        copy: {
            bootstrap_fonts: {
                expand: true,
                cwd: 'public/bower_components/bootstrap/fonts/',
                src: '**',
                dest: 'public/fonts/'
            }
        }
    });

    grunt.loadNpmTasks('grunt-exec');
    grunt.loadNpmTasks('grunt-injector');
    grunt.loadNpmTasks('grunt-bower-concat');
    grunt.loadNpmTasks('grunt-contrib-concat');
    grunt.loadNpmTasks('grunt-contrib-watch');
    grunt.loadNpmTasks('grunt-contrib-jshint');
    grunt.loadNpmTasks('grunt-contrib-uglify');
    grunt.loadNpmTasks('grunt-contrib-cssmin');
    grunt.loadNpmTasks('grunt-contrib-clean');
    grunt.loadNpmTasks('grunt-contrib-copy');

    grunt.registerTask('customize-bootstrap', 'Customize bootstraps less variables!', function () {
        var path = './public/bower_components/bootstrap/less/bootstrap.less';
        if (!grunt.file.exists(path)) {
            grunt.log.error("Can't customize bootstrap: file " + path + " not found!");
            return false;
        }
        var file = grunt.file.read(path);
        var customize = '@import "../../../less/customizeBootstrap.less";';
        if (file.search(customize) < 0) {
            file = file.replace('@import "variables.less";', '@import "variables.less";\n' + customize);
            grunt.file.write(path, file);
            grunt.log.ok('bootstrap.less customized!');
        } else {
            grunt.log.ok('bootstrap.less already customized!');
        }
        return true;
    });

    grunt.registerTask('twbs', ['customize-bootstrap', 'exec:twbs_compile', 'copy:bootstrap_fonts']);
    grunt.registerTask('test', ['clean', 'jshint', 'watch']);
    grunt.registerTask('dev', ['clean', 'jshint', 'injector:dev']);
    grunt.registerTask('prod', ['clean', 'twbs', 'concat', 'bower_concat', 'uglify', 'cssmin', 'injector:prod']);
    grunt.registerTask('default', ['dev']);
};