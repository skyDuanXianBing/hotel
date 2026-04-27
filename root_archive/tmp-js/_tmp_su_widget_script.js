function loadScript(n) {
  var i = {
    "channel-mapping":
      "https://partner.su-api.com/channel-mapping/main.js?tmp=" +
      new Date().getTime(),
    "revenue-report":
      "https://extranet.suissu.com/revenue-report/main.js?tmp=" +
      new Date().getTime(),
    "booking-details":
      "https://partner.su-api.com/booking-details/main.js?tmp=" +
      new Date().getTime(),
    mbs: "https://partner.su-api.com/mbs/main.js?tmp=" + new Date().getTime(),
  };
  let {
    elementId: r = "",
    type: o = "channel-Mapping",
    appId: d = "",
    propertyId: s = "",
    token: a = "",
    channelCode: p = "",
    themeColor: c = {},
    language: m = "en",
    config: u = {},
  } = n || {};
  var n = (u || {}).readonlyChannelGroup,
    l = document.getElementById(r);
  if (l) {
    let e = "";
    if (
      ("revenue-report" === o
        ? (d && a) || (e = "appId and token is required")
        : "channel-mapping" === o
        ? (d && a && (n || s)) ||
          (e = "appId, token and propertyId is required")
        : (d && a && s) || (e = "appId, token and propertyId is required"),
      e)
    )
      l.innerHTML = e;
    else {
      let t = {};
      Object.keys(c || {}).forEach((e) => {
        /^#[0-9A-F]{6}$/i.test(c[e]) && (t[e] = c[e]);
      }),
        l.setAttribute("appId", d),
        l.setAttribute("token", a),
        l.setAttribute("propertyId", s),
        l.setAttribute("channelCode", p),
        l.setAttribute("language", m),
        l.setAttribute("themeColor", JSON.stringify(t)),
        l.setAttribute("config", JSON.stringify(u));
      document.getElementById("widget-script") &&
        document.body.removeChild(document.getElementById("widget-script")),
        document.getElementById("widget-react-script") ||
          (((n = document.createElement("script")).id = "widget-react-script"),
          (n.src =
            "https://unpkg.com/react@18.2.0/umd/react.production.min.js"),
          (n.crossOrigin = "anonymous"),
          document.body.appendChild(n)),
        document.getElementById("widget-react-dom-script") ||
          (((l = document.createElement("script")).id =
            "widget-react-dom-script"),
          (l.src =
            "https://unpkg.com/react-dom@18.2.0/umd/react-dom.production.min.js"),
          (l.crossOrigin = "anonymous"),
          document.body.appendChild(l));
      n = document.createElement("script");
      (n.id = "widget-script"),
        (n.src = i[o.toLowerCase()] || i["channel-mapping"]),
        (n.async = !0),
        n.setAttribute("data-element", r),
        document.body.appendChild(n);
    }
  } else console.error("div not found");
}

function loadSuWidget(e) {
  loadScript(e);
}

