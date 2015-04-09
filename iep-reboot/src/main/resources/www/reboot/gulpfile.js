// gulp packages
var gulp = require('gulp');
var rename = require('gulp-rename');
var concat = require('gulp-concat');
var stylus = require('gulp-stylus');
var minifyCss = require('gulp-minify-css');
var sourcemaps = require('gulp-sourcemaps');
var autoprefixer = require('gulp-autoprefixer');
var zip = require('gulp-zip');
var connect = require('gulp-connect');
var vulcanize = require('gulp-vulcanize');

// config vars
var twoNewLines = '\r\n\r\n\r\n';
var cssSource = ['./css/src/style.styl'];
var cssWatch = ['./css/src/*.styl'];
var cssDestination = './css/dist/';

// temporary kludge for working with components (update to point to the relevant component as required)
var cssComponentSource = './components/nf-lightbox/nf-lightbox.styl';
var cssComponentDestination = './components/nf-lightbox/';

gulp.task('css', function() {
     gulp.src(cssSource)
    .pipe(stylus({compress: false}))
    .pipe(sourcemaps.init())
    .pipe(concat('reboot.css', {newLine: twoNewLines}))
    .pipe(autoprefixer({
        browsers: ['last 1 version']
    }))
    .pipe(gulp.dest(cssDestination))
    .pipe(minifyCss())
    .pipe(rename({extname: '.min.css'}))
    .pipe(sourcemaps.write('.'))
    .pipe(gulp.dest(cssDestination))
    .pipe(connect.reload());
});

gulp.task('componentCSS', function() {
    gulp.src(cssComponentSource)
        .pipe(stylus({compress: false}))
        .pipe(autoprefixer({
            browsers: ['last 1 version']
        }))
        .pipe(gulp.dest(cssComponentDestination))
        .pipe(connect.reload());
});

gulp.task('starterKit', function() {
     gulp.src('starterKit/*')
    .pipe(zip('starterKit.zip'))
    .pipe(gulp.dest('downloads'));
});

gulp.task('build', ['css', 'starterKit']);

gulp.task('connectDev', function () {
    connect.server({
        port: 8080,
        livereload: true
    });
});

gulp.task('watch', function () {
    gulp.watch(cssWatch, ['css']);
});

gulp.task('componentWatch', function () {
    gulp.watch(cssComponentSource, ['componentCSS']);
});

gulp.task('default', ['connectDev', 'watch']);

gulp.task('component', ['connectDev', 'componentWatch']);

gulp.task('vulcanize', function () {
    var DEST_DIR = '.';

    return gulp.src('imports.html')
        .pipe(rename('netflix.html'))
        .pipe(vulcanize({
            dest: DEST_DIR,
            csp: true,
            inline: true,
            strip: true
        }))
        .pipe(gulp.dest(DEST_DIR));
});