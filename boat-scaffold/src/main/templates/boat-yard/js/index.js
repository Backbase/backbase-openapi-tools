window.onload = function () {
  const iframe = document.getElementById("iframe");
  const hash = window.location.hash;
  if (hash) {
    iframe.src = window.location.hash.replace("#", "");
  }

  setTimeout(() => {
    iframe.style.visibility = "visible";
    iframe.style.display = "flex";
  }, 100);

  document.querySelectorAll("#nav a").forEach(($e) => {
    $e.addEventListener("click", function (e) {
      const html = e.target.outerHTML;

      const m = html.match(/href=\"(.*?)\"/);
      url = null;
      if (m) url = m[1];
      window.location.hash = url;
    });
  });
};
