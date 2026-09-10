(function () {
  var el = document.getElementById("api-status");
  var done = false;
  function show(msg) {
    if (done) return;
    done = true;
    if (el) el.textContent = msg;
  }
  var timer = setTimeout(function () {
    show("API did not respond. Close old Java windows and run run-local.bat, then open http://127.0.0.1:8080/");
  }, 4000);
  fetch("/api/health").then(function (r) {
    clearTimeout(timer);
    show(r.ok
      ? ("API is up at " + location.origin)
      : ("API returned " + r.status + ". Restart run-local.bat and open http://127.0.0.1:8080/"));
  }).catch(function () {
    clearTimeout(timer);
    show("Cannot reach the API. Run run-local.bat and open http://127.0.0.1:8080/");
  });
})();
