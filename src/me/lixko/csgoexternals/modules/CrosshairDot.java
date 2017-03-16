package me.lixko.csgoexternals.modules;

import me.lixko.csgoexternals.util.DrawUtils;

public class CrosshairDot extends Module {
	
    private final int sx = (int) (DrawUtils.getWidth() * 0.5f);
    private final int sy = (int) (DrawUtils.getHeight() * 0.5f);
    
	@Override
	public void onUIRender() {
		DrawUtils.setColor(0, 0, 0, 150);
	    DrawUtils.fillRectanglew(sx-2, sy-2, 4, 4);
	    DrawUtils.setColor(0x00FFFFFF);
	    DrawUtils.fillRectanglew(sx-1, sy-1, 2, 2);
	}

}
