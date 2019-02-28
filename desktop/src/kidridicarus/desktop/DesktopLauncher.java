package kidridicarus.desktop;

import com.badlogic.gdx.backends.lwjgl.LwjglApplication;
import com.badlogic.gdx.backends.lwjgl.LwjglApplicationConfiguration;

import kidridicarus.game.MyKidRidicarus;
import kidridicarus.game.info.GfxInfo;

public class DesktopLauncher {
	public static void main (String[] arg) {
		LwjglApplicationConfiguration config = new LwjglApplicationConfiguration();
		config.width = GfxInfo.V_WIDTH * GfxInfo.DESKTOP_SCALE;
		config.height = GfxInfo.V_HEIGHT * GfxInfo.DESKTOP_SCALE;
		new LwjglApplication(new MyKidRidicarus(), config);
	}
}