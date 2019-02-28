package kidridicarus.game.screen;

import com.badlogic.gdx.Game;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.InputProcessor;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Label.LabelStyle;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.utils.viewport.FitViewport;
import com.badlogic.gdx.utils.viewport.Viewport;

import kidridicarus.game.MyKidRidicarus;
import kidridicarus.game.info.GfxInfo;

public class GameOverScreen implements Screen {
	private Viewport viewport;
	private Stage stage;
	private Game game;
	private InputProcessor oldInPr;
	private boolean didAnythingHappen;

	public GameOverScreen(Game game, boolean win) {
		LabelStyle font;
		Label gameOverLabel, playAgainLabel;
		Table table;

		this.game = game;
		viewport = new FitViewport(GfxInfo.V_WIDTH, GfxInfo.V_HEIGHT, new OrthographicCamera());
		stage = new Stage(viewport, ((MyKidRidicarus) game).batch);

		font = new LabelStyle(new BitmapFont(), Color.WHITE);
		table = new Table();
		table.center();
		table.setFillParent(true);

		if(win)
			gameOverLabel = new Label("GAME WON!", font);
		else
			gameOverLabel = new Label("GAME OVER", font);
		playAgainLabel = new Label("Do Something to Play Again", font);

		table.add(gameOverLabel).expandX();
		table.row();
		table.add(playAgainLabel).expandX().padTop(10f);

		stage.addActor(table);

		didAnythingHappen = false;
		oldInPr = Gdx.input.getInputProcessor();
		Gdx.input.setInputProcessor(new MyLittleInPr());
	}

	private class MyLittleInPr implements InputProcessor {
		private boolean a() { return didAnythingHappen = true; }
		// return true for all the following to relay that the event was handled
		@Override
		public boolean keyDown(int keycode) { return a(); }
		@Override
		public boolean keyUp(int keycode) { return a(); }
		@Override
		public boolean keyTyped(char character) { return a(); }
		@Override
		public boolean touchDown(int screenX, int screenY, int pointer, int button) { return a(); }
		@Override
		public boolean touchUp(int screenX, int screenY, int pointer, int button) { return a(); }
		@Override
		public boolean touchDragged(int screenX, int screenY, int pointer) { return a(); }
		@Override
		public boolean mouseMoved(int screenX, int screenY) { return a(); }
		@Override
		public boolean scrolled(int amount) { return a(); }
	}

	@Override
	public void show() {
	}

	@Override
	public void render(float delta) {
		if(didAnythingHappen) {
			game.setScreen(new PlayScreen((MyKidRidicarus) game, 0));
			dispose();
		}

		Gdx.gl.glClearColor(0,  0,  0,  1);
		Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);
		stage.draw();
	}

	@Override
	public void resize(int width, int height) {
	}

	@Override
	public void pause() {
	}

	@Override
	public void resume() {
	}

	@Override
	public void hide() {
	}

	@Override
	public void dispose() {
		Gdx.input.setInputProcessor(oldInPr);
		stage.dispose();
	}
}