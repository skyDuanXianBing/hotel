function loadScript(n) {
  var i = {
    "arr":
      "https://partner.su-api.com/arr/main.js?tmp=" +
      new Date().getTime(),
    "channel-mapping":
      "https://partner.su-api.com/channel-mapping/main.js?tmp=" +
      new Date().getTime(),
    "revenue-report":
      "https://partner.su-api.com/revenue-report/main.js?tmp=" +
      new Date().getTime(),
    "booking-details":
      "https://partner.su-api.com/booking-details/main.js?tmp=" +
      new Date().getTime(),
    mbs: "https://partner.su-api.com/mbs/main.js?tmp=" + new Date().getTime(),
  };
  let {
    elementId: r = "",
    type: s = "channel-Mapping",
    appId: o = "",
    propertyId: d = "",
    token: a = "",
    channelCode: p = "",
    themeColor: m = {},
    language: c = "en",
    config: u = {},
    isSuperAdmin: l = !1,
  } = n || {};
  var n = (u || {}).readonlyChannelGroup,
    g = document.getElementById(r);
  if (g) {
    let e = "";
    if (
      ("revenue-report" === s
        ? (e = "")
        : "channel-mapping" === s
          ? (o && a && (n || d)) ||
            (e = "appId, token and propertyId is required")
          : (o && a && d) || (e = "appId, token and propertyId is required"),
      e)
    )
      g.innerHTML = e;
    else {
      let t = {};
      (Object.keys(m || {}).forEach((e) => {
        /^#[0-9A-F]{6}$/i.test(m[e]) && (t[e] = m[e]);
      }),
        g.setAttribute("appId", o),
        g.setAttribute("token", a),
        g.setAttribute("propertyId", d),
        g.setAttribute("channelCode", p),
        g.setAttribute("language", c),
        g.setAttribute("themeColor", JSON.stringify(t)),
        g.setAttribute("config", JSON.stringify(u)),
        "revenue-report" === s && g.setAttribute("isSuperAdmin", l));
      (document.getElementById("widget-script") &&
        document.body.removeChild(document.getElementById("widget-script")),
        document.getElementById("widget-react-script") ||
          (((n = document.createElement("script")).id = "widget-react-script"),
          (n.src =
            "https://unpkg.com/react@18.2.0/umd/react.production.min.js"),
          (n.crossOrigin = "anonymous"),
          document.body.appendChild(n)),
        document.getElementById("widget-react-dom-script") ||
          (((g = document.createElement("script")).id =
            "widget-react-dom-script"),
          (g.src =
            "https://unpkg.com/react-dom@18.2.0/umd/react-dom.production.min.js"),
          (g.crossOrigin = "anonymous"),
          document.body.appendChild(g)));
      n = document.createElement("script");
      ((n.id = "widget-script"),
        (n.src = i[s.toLowerCase()] || i["channel-mapping"]),
        (n.async = !0),
        n.setAttribute("data-element", r),
        document.body.appendChild(n));
    }
  } else console.error("div not found");
}

function loadSuWidget(e) {
  loadScript(e);
}
