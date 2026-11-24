// Fills the build line and the release history from the very files the site serves, so a page can
// never advertise a version, size or checksum that the download button doesn't hand out.
//
// Markup hooks: an element with data-build="release/version.json" holds the spans [data-version],
// [data-build], [data-date], [data-size] and [data-sha]; an element with
// data-releases="release/releases.json" gets one <li> per build. Everything is replaced only after
// a successful fetch: with JavaScript off, or the file missing, the static text stays.
(function () {
  'use strict';

  function fill(root, name, value) {
    if (!value) return;
    root.querySelectorAll('[data-' + name + ']').forEach(function (el) {
      el.textContent = value;
    });
  }

  function kb(bytes) {
    return bytes > 0 ? Math.round(bytes / 1024) + ' KB' : '';
  }

  function lang() {
    return (document.documentElement.lang || 'en').toLowerCase();
  }

  var build = document.querySelector('[data-build]');
  if (build && window.fetch) {
    fetch(build.getAttribute('data-build'), { cache: 'no-store' })
      .then(function (r) { return r.ok ? r.json() : Promise.reject(r.status); })
      .then(function (v) {
        fill(build, 'version', v.versionName ? 'v' + v.versionName : '');
        fill(build, 'build', v.build ? 'build ' + v.build : '');
        fill(build, 'date', v.releaseDate || '');
        fill(build, 'size', kb(v.apkSize));
        fill(build, 'sha', v.apkSha256 || '');
      })
      .catch(function () { /* keep the static text */ });
  }

  var list = document.querySelector('[data-releases]');
  if (list && window.fetch) {
    fetch(list.getAttribute('data-releases'), { cache: 'no-store' })
      .then(function (r) { return r.ok ? r.json() : Promise.reject(r.status); })
      .then(function (entries) {
        if (!Array.isArray(entries) || !entries.length) return;
        var fragment = document.createDocumentFragment();
        entries.slice(0, 12).forEach(function (e) {
          var item = document.createElement('li');
          item.className = 'entry';

          var head = document.createElement('div');
          head.className = 'entry-head';
          var name = document.createElement('strong');
          name.textContent = 'SwadayaTV ' + (e.versionName || '');
          head.appendChild(name);
          if (e.build) {
            var b = document.createElement('span');
            b.textContent = 'build ' + e.build;
            head.appendChild(b);
          }
          if (e.releaseDate) {
            var when = document.createElement('time');
            when.dateTime = e.releaseDate;
            var date = new Date(e.releaseDate + 'T00:00:00');
            when.textContent = isNaN(date.getTime()) ? e.releaseDate
              : date.toLocaleDateString(lang() === 'id' ? 'id-ID' : 'en-GB',
                { day: 'numeric', month: 'short', year: 'numeric' });
            head.appendChild(when);
          }
          item.appendChild(head);

          if (e.changelog) {
            var note = document.createElement('p');
            note.textContent = e.changelog;
            item.appendChild(note);
          }
          if (e.apkSha256) {
            var sha = document.createElement('p');
            sha.className = 'mono';
            sha.textContent = 'SHA-256 ' + e.apkSha256 + (e.apkSize ? ' · ' + kb(e.apkSize) : '');
            item.appendChild(sha);
          }
          fragment.appendChild(item);
        });
        list.textContent = '';
        list.appendChild(fragment);
      })
      .catch(function () { /* keep the static text */ });
  }
})();
