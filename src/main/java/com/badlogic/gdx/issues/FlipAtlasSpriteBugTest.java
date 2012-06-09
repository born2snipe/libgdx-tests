package com.badlogic.gdx.issues;

import java.util.ArrayList;

import com.badlogic.gdx.Game;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input.Keys;
import com.badlogic.gdx.backends.lwjgl.LwjglApplication;
import com.badlogic.gdx.backends.lwjgl.LwjglApplicationConfiguration;
import com.badlogic.gdx.graphics.GL10;
import com.badlogic.gdx.graphics.g2d.Sprite;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureAtlas;
import com.badlogic.gdx.graphics.g2d.TextureAtlas.AtlasSprite;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer.ShapeType;
import com.badlogic.gdx.math.Rectangle;

public class FlipAtlasSpriteBugTest extends Game {

	private SpriteBatch spriteBatch;

	private ArrayList<Sprite> sprites;
	private TextureAtlas textureAtlas;
	private ShapeRenderer shapeRenderer;

	private AtlasSprite sprite2;

	@Override
	public void create() {
		spriteBatch = new SpriteBatch();
		shapeRenderer = new ShapeRenderer();
		sprites = new ArrayList<Sprite>();
		
		textureAtlas = new TextureAtlas(Gdx.files.internal("data/test/pack"), Gdx.files.internal("data/test"));

		sprite2 = (AtlasSprite) textureAtlas.createSprite("something2");
		sprite2.setPosition(200f, 200f);
		
		sprites.add(sprite2);
		
	}

	@Override
	public void dispose() {
		super.dispose();
		spriteBatch.dispose();
		textureAtlas.dispose();
		shapeRenderer.dispose();
	}

	@Override
	public void render() {
		Gdx.gl.glClearColor(0f, 0f, 1f, 1f);
		Gdx.gl.glClear(GL10.GL_COLOR_BUFFER_BIT);

		spriteBatch.begin();
		for (int i = 0; i < sprites.size(); i++) {
			Sprite sprite = sprites.get(i);
			sprite.draw(spriteBatch);
			{
				Rectangle boundingRectangle = sprite.getBoundingRectangle();
				shapeRenderer.begin(ShapeType.Rectangle);
				shapeRenderer.setColor(1f, 1f, 1f, 1f);
				shapeRenderer.rect(boundingRectangle.getX(), boundingRectangle.getY(), boundingRectangle.getWidth(), boundingRectangle.getHeight());
				shapeRenderer.end();
			}
		}
		spriteBatch.end();

		if (Gdx.input.justTouched()) {
			sprite2.flip(false, true);
		}

		if (Gdx.input.isKeyPressed(Keys.SPACE)) {
			dispose();
			create();
		}

	}

	public static void main(String[] args) {
		LwjglApplicationConfiguration config = new LwjglApplicationConfiguration();

		config.title = "SpriteAtlasRotateTest";
		config.width = 800;
		config.height = 600;
		config.fullscreen = false;
		config.useGL20 = false;
		config.useCPUSynch = true;
		config.forceExit = true;
		config.vSyncEnabled = true;

		new LwjglApplication(new FlipAtlasSpriteBugTest(), config);
	}
}
