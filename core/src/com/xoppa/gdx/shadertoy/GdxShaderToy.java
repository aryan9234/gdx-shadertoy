package com.xoppa.gdx.shadertoy;

import com.badlogic.gdx.*;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.graphics.FPSLogger;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.glutils.ShaderProgram;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.badlogic.gdx.utils.*;
import com.badlogic.gdx.utils.StringBuilder;
import com.badlogic.gdx.utils.viewport.ScreenViewport;
import com.kotcrab.vis.ui.VisUI;
import com.kotcrab.vis.ui.widget.Menu;
import com.kotcrab.vis.ui.widget.MenuBar;
import com.kotcrab.vis.ui.widget.MenuItem;
import com.kotcrab.vis.ui.widget.file.FileChooser;
import com.kotcrab.vis.ui.widget.file.FileChooserListener;
import com.kotcrab.vis.ui.widget.file.FileTypeFilter;
import com.kotcrab.vis.ui.widget.file.internal.PreferencesIO;
import javafx.scene.input.KeyCode;

import static com.badlogic.gdx.Gdx.files;

public class GdxShaderToy extends ApplicationAdapter {
	Stage stage;
	Texture img;
	FullQuadToy toy;
	CollapsableTextWindow logWindow;
	CollapsableTextWindow vsWindow;
	CollapsableTextWindow fsWindow;
	float codeChangedTimer = -1f;
	long startTimeMillis;
	long fpsStartTimer;
	Logger logger;

	@Override
	public void create () {
		Pref.init();
		ShaderProgram.pedantic = false;
		startTimeMillis = TimeUtils.millis();
		fpsStartTimer = TimeUtils.nanoTime();
		img = new Texture(files.internal("badlogic.jpg"));
		img.setWrap(Texture.TextureWrap.Repeat, Texture.TextureWrap.Repeat);
		VisUI.load();
		stage = new Stage(new ScreenViewport());

		Table root=new Table();
		root.setFillParent(true);
		stage.addActor(root);

		Gdx.input.setInputProcessor(stage);
		stage.addListener(new ClickListener(){

			@Override
			public boolean keyDown(InputEvent event, int keycode) {

				if(Gdx.app.getType()== Application.ApplicationType.Desktop){
					if(Gdx.input.isKeyPressed(Input.Keys.CONTROL_LEFT)){
						if(keycode== Input.Keys.S){
							saveShaderOnFile();
						}
					}
				}
				return super.keyDown(event, keycode);
			}
		});

		logger = new Logger("TEST", Logger.INFO) {
			com.badlogic.gdx.utils.StringBuilder sb = new StringBuilder();
			private void add(String message) {
				long time = TimeUtils.timeSinceMillis(startTimeMillis);
				long s = time / 1000;
				long m = s / 60;
				long h = m / 60;
				sb.setLength(0);
				sb.append(h, 2).append(':').append(m % 60, 2).append(':').append(s % 60, 2).append('.').append(time % 1000, 3);
				sb.append(" ").append(message).append("\n");
				logWindow.addText(sb.toString());
			}

			@Override
			public void info(String message) {
				super.info(message);
				//add("[#ffff00]" + message + "[]");
				add("INFO: " + message);
			}

			@Override
			public void error(String message) {
				super.error(message);
				//add("[red]" + message + "[]");
				add("ERROR: " + message);
			}
		};

		toy = new FullQuadToy();
		toy.create(logger);
		toy.setTexture(img);

		ChangeListener codeChangeListener = new ChangeListener() {
			@Override
			public void changed(ChangeEvent event, Actor actor) {
				codeChangedTimer = 3f;
			}
		};

		float w = Gdx.graphics.getWidth();
		float h = Gdx.graphics.getHeight();
		float hw = w * 0.5f;
		logWindow = new CollapsableTextWindow("Log", 0, 0, w, 100f);
		root.addActor(logWindow);

        final String defaultVS = "";
		final String defaultFS = "";

		vsWindow = new CollapsableTextWindow("Vertex Shader", 0, 100f, hw, h - 100f);
		vsWindow.setText(defaultVS);
		vsWindow.addTextAreaListener(codeChangeListener);

		FileHandle fileHandle=files.internal("shaders/default.vertex.glsl");
		String array[]=fileHandle.readString().split("\n");
		for (int j=0;j<array.length;j++){
			vsWindow.addText(array[j]);
		}

		root.addActor(vsWindow);
		fsWindow = new CollapsableTextWindow("Fragment Shader", hw, 100f, hw, h - 100f);
		fsWindow.setText(defaultFS);
		fsWindow.addTextAreaListener(codeChangeListener);

		if(Pref.hasCurrentFile()){
			fileHandle=new FileHandle(Pref.getCurrentFile());
			if(fileHandle.exists()){
				array=fileHandle.readString().split("\n");
				for (int j=0;j<array.length;j++){
					fsWindow.addText(array[j]);
				}
				fsWindow.getTitleLabel().setText(Pref.getCurrentFile());
				toy.setShader(vsWindow.getText(), fsWindow.getText());
			}else {
				Pref.currentFileStatus(false);
			}
		}

		root.addActor(fsWindow);
		MenuBar menuBar=new MenuBar();
		Menu info=new Menu("File");
		menuBar.addMenu(info);
		root.add(menuBar.getTable()).expandX().fillX().row();
		root.add().expand().fill().row();

		if(Gdx.app.getType()==Application.ApplicationType.Desktop) {

			PreferencesIO.setDefaultPrefsName(Constants.PREF);
			Gdx.app.getPreferences(Constants.PREF);

			PreferencesIO preferencesIO=new PreferencesIO();

			final FileChooser fileChooser = new FileChooser(FileChooser.Mode.OPEN);
			fileChooser.setSize(w, h);

			FileTypeFilter fileTypeFilter = new FileTypeFilter(true);
			fileTypeFilter.addRule(".fragment.glsl", "glsl");

			fileChooser.setFileTypeFilter(fileTypeFilter);
			MenuItem open, save;
			info.addItem(open = new MenuItem("Open"));
			info.addItem(save = new MenuItem("Save"));

			open.addListener(new ChangeListener() {
				@Override
				public void changed(ChangeEvent changeEvent, Actor actor) {

					if(Pref.hasCurrentFile()){
						FileHandle fileHandle1=new FileHandle(Pref.getCurrentFile());
						if(fileHandle1.exists())
						fileChooser.setDirectory(fileHandle1.file().getParent());
					}
					stage.addActor(fileChooser.fadeIn());
				}
			});

			save.addListener(new ChangeListener() {
				@Override
				public void changed(ChangeEvent changeEvent, Actor actor) {
					saveShaderOnFile();
				}
			});

			fileChooser.setListener(new FileChooserListener() {
				@Override
				public void selected(Array<FileHandle> files) {
					fsWindow.getTitleLabel().setText(files.get(0).toString());

					FileHandle fileHandle=new FileHandle(files.get(0).toString());
					Pref.saveCurrentFile(files.get(0).toString());
					String fileExt=fileHandle.extension();
					if(fileExt.equals("glsl")) {
						fsWindow.setText(defaultFS);
						String array[]=fileHandle.readString().split("\n");
						for (int j=0;j<array.length;j++){
							fsWindow.addText(array[j]);
						}
						toy.setShader(vsWindow.getText(), fsWindow.getText());
					}
				}

				@Override
				public void canceled() {
				}
			});
		}else if(Gdx.app.getType()== Application.ApplicationType.Android){

			array=files.internal("shaders/startup.fragment.glsl").readString().split("\n");
			for (int j=0;j<array.length;j++){
				fsWindow.addText(array[j]);
			}
			toy.setShader(vsWindow.getText(), fsWindow.getText());
		}

		//toy.setShader(defaultVS, defaultFS);
	}

	@Override
	public void resize(int width, int height) {
		stage.getViewport().update(width, height);
		toy.resize(width, height);
	}

	@Override
	public void render () {
        update();
		Gdx.gl.glClearColor(0, 0, 0, 1);
		Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT | GL20.GL_DEPTH_BUFFER_BIT);
		toy.render();
		stage.act();
		stage.draw();
	}

    private void update() {
        if (codeChangedTimer > 0f) {
            codeChangedTimer -= Gdx.graphics.getDeltaTime();
            if (codeChangedTimer <= 0) {
				toy.setShader(vsWindow.getText(), fsWindow.getText());
            }
        }
		if (TimeUtils.nanoTime() - fpsStartTimer > 1000000000) /* 1,000,000,000ns == one second */{
			logger.info("fps: " + Gdx.graphics.getFramesPerSecond());
			fpsStartTimer = TimeUtils.nanoTime();
		}
    }

    private void saveShaderOnFile(){

		if(Pref.hasCurrentFile()){
			FileHandle fileHandle1=	new FileHandle(fsWindow.getTitleLabel().getText().toString());
			fileHandle1.writeString(fsWindow.getText(),false);
		}
	}

	@Override
	public void dispose() {
		VisUI.dispose();
		stage.dispose();
		img.dispose();
	}
}
