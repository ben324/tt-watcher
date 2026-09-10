document.addEventListener("DOMContentLoaded", function () {
  var r = document.getElementById("s-radius");
  var v = document.getElementById("s-radius-val");
  if (!r) return;
  function sync() { if (v) v.textContent = r.value; }
  r.addEventListener("input", sync);
  r.addEventListener("change", sync);
  sync();
});
