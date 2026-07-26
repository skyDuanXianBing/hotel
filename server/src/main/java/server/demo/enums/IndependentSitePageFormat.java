package server.demo.enums;

/**
 * 独立站页面内容格式：BLOCKS 为旧的区块式 schema（independent_site_page_v1），
 * CANVAS 为自由节点树 schema（independent_site_canvas_v1）。
 * 校验、生成、编辑、公开渲染全部按该字段分派。
 */
public enum IndependentSitePageFormat {
    BLOCKS,
    CANVAS
}
