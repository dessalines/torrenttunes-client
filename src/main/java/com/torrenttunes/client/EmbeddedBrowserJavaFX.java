package com.torrenttunes.client;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import javafx.application.Application;
import javafx.event.EventHandler;
import javafx.geometry.HPos;
import javafx.geometry.VPos;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.paint.Color;
import javafx.scene.web.WebEngine;
import javafx.scene.web.WebView;
import javafx.stage.Stage;


public class EmbeddedBrowserJavaFX extends Application implements Runnable {
	private Scene scene;
	@Override public void start(Stage stage) {
		
		System.setProperty("prism.lcdtext", "true");
		
		
		// create the scene
		stage.setTitle(DataSources.APP_NAME);
		scene = new Scene(new Browser(),750,500, Color.web("#666970"));
		stage.setScene(scene);
		//        scene.getStylesheets().add("webviewsample/BrowserToolbar.css");  

		// TODO set icon
		//        stage.getIcons().add(new Image("/path/to/stackoverflow.jpg"));


		stage.setMaximized(true);
		stage.show();

		stage.setOnCloseRequest(e -> System.exit(0));


	}

	public void run() {
		launch(null);
	}

	public static void start(){

		EmbeddedBrowserJavaFX e = new EmbeddedBrowserJavaFX();
		Thread thread = new Thread(e);
		thread.start();
	}

	private static void main(String[] args) {
		launch(args);
	}
}

class Browser extends Region {

	final WebView browser = new WebView();
	final WebEngine webEngine = browser.getEngine();

	public Browser() {
		//apply the styles
		getStyleClass().add("browser");
		// load the web page
		webEngine.load(DataSources.MAIN_PAGE_URL());
		//add the web view to the scene
		getChildren().add(browser);


	}
	private Node createSpacer() {
		Region spacer = new Region();
		HBox.setHgrow(spacer, Priority.ALWAYS);
		return spacer;

	}

	@Override protected void layoutChildren() {
		double w = getWidth();
		double h = getHeight();
		layoutInArea(browser,0,0,w,h,0, HPos.CENTER, VPos.CENTER);
	}

	@Override protected double computePrefWidth(double height) {
		return 20;
		//        return 750;
	}

	@Override protected double computePrefHeight(double width) {
		return 500;
	}
}