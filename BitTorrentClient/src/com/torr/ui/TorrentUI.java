package com.torr.ui;

import java.io.File;

import javafx.application.*;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.collections.ObservableList;
import javafx.event.*;
import javafx.geometry.*;
import javafx.scene.*;
import javafx.stage.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.control.*;
import javafx.scene.text.*;
import com.torr.client.*;

public class TorrentUI extends Application implements ITorrentUI {
	
	private Text statusBarText = null;
	private Text fileNameText = null;
	private Text infoHashText = null;
	private Text numberOfPiecesText = null;
	private Text downloadedPiecesText = null;
	private Text downloadSpeedText = null;
	private Text peersNumberText = null;
	private ScrollPane consoleWrapper = null;
	private VBox detailsConsole = null;
	private boolean scrollToBottom = false;
	private TorrentMain torrentFiles;
	
	public static void main(String[] args) {
		System.out.println("Launching JavaFX application.");		
		
		launch(args);

	}	
	
	@Override
	public void init() {
		System.out.println("Inside the init() method.");		
	}
	
	@Override
	public void start(Stage myStage) {
		System.out.println("Inside the start() method.");
		
		// Window title
		myStage.setTitle("BitTorrent Client");

	    // Vertically-aligned main content
	    VBox contentBox = new VBox();
	    VBox detailsBox = createDetailsBox();
	    
		contentBox.getChildren().addAll(createFileOpenPane(), detailsBox);
		VBox.setVgrow(detailsBox, Priority.ALWAYS);
		
		// The main screen pane
		BorderPane screenPane = new BorderPane();
		screenPane.setTop(createMenuBar());
		screenPane.setBottom(createStatusBar());		
		screenPane.setCenter(contentBox);
		
		// Create a scene 
		Scene myScene = new Scene(screenPane, 640, 520);
		
		// Set the scene on the stage
		myStage.setScene(myScene);
		
		// Show the stage and its scene.
		myStage.show();	
		
		this.torrentFiles = new TorrentMain(this);
		
		
	}
	
	//private void Create
	
	
	@Override
	public void stop() {
		System.out.println("Inisde the stop() method.");
	}
	
	/*
	 * 
	 * Thread safe methods 
	 * 
	 */	
	@Override
	public void setStatusBarText(final String text)
	{
        Platform.runLater(new Runnable() {

            @Override
            public void run() {
            	setStatusBarTextInternal(text);
            }
        });				
	}
	@Override
	public void setFileName(final String text)
	{
        Platform.runLater(new Runnable() {

            @Override
            public void run() {
            	setFileNameInternal(text);
            }
        });		
	}	
	@Override
	public void setInfoHash(final String text)
	{
        Platform.runLater(new Runnable() {

            @Override
            public void run() {
            	setInfoHashInternal(text);
            }
        });	
	}
	@Override
	public void setNumberOfPieces(final String text)
	{
        Platform.runLater(new Runnable() {

            @Override
            public void run() {
            	setNumberOfPiecesInternal(text);
            }
        });	
	}
	@Override
	public void setDownloadedPieces(final String text)
	{
        Platform.runLater(new Runnable() {

            @Override
            public void run() {
            	setDownloadedPiecesInternal(text);
            }
        });	
	}
	@Override
	public void setDownloadSpeed(final int bps)
	{
        Platform.runLater(new Runnable() {
        	
            @Override
            public void run() {
            	
            	setDownloadSpeedInternal(bps);
            }
        });	
	}
	@Override
	public void setPeersNumber(final String text)
	{
        Platform.runLater(new Runnable() {

            @Override
            public void run() {
            	setPeersNumberInternal(text);
            }
        });	
	}	
	@Override
	public void printConsoleInfo(final String text)
	{
        Platform.runLater(new Runnable() {

            @Override
            public void run() {
            	printConsoleInfoInternal(text);
            }
        });	
	}		
	
	private void addControls(ObservableList<Node> rootNodeChildren) {
		Button btnOpen = new Button("Open Torrent File");
		
		// Add handlers to the buttons
		btnOpen.setOnAction(new EventHandler<ActionEvent>() {
			public void handle(ActionEvent ae) {
				openTorrent();
			}
		});
		
		
		// Add all controls to the scene graph
		rootNodeChildren.addAll(btnOpen);
	}
	
	private HBox createStatusBar()
	{
		HBox statusBar = new HBox();
		statusBar.setStyle("-fx-background-color: gainsboro");
		statusBar.setPadding(new Insets(0, 0, 0, 5));
		this.statusBarText = new Text("Ready...");
		statusBar.getChildren().add(this.statusBarText);
			
		return statusBar;
	}
	
	private MenuBar createMenuBar()
	{
	    MenuBar menuBar = new MenuBar();
	    
	    Menu fileMenu = new Menu("File");
	    MenuItem openMenu = new MenuItem("Open Torrent File");
	    MenuItem exitMenu = new MenuItem("Exit");
	    
	   // MenuItem levelEditMenu = new MenuItem("Edit Levels");
	    fileMenu.getItems().add(openMenu);
	    fileMenu.getItems().add(new SeparatorMenuItem());
	    fileMenu.getItems().add(exitMenu);
	    menuBar.getMenus().add(fileMenu);		
	    
	    
	    Menu helpMenu = new Menu("Help");
	    MenuItem aboutMenu = new MenuItem("About");
	    
	    helpMenu.getItems().add(aboutMenu);

	    menuBar.getMenus().add(helpMenu);
	    
	    return menuBar;
	}
		
	
	private VBox createDetailsBox()
	{
		VBox detailsBox = new VBox();
		detailsBox.setStyle("-fx-background-color: #ffffff;");
		
		Font titlesFont = Font.font("Arial", FontWeight.BOLD, 20);
		
		HBox titleBox = new HBox();
		titleBox.setAlignment(Pos.CENTER);
		titleBox.setPadding(new Insets(10, 0, 0, 0));
		
		Text title = new Text("File Details");
		title.setFont(titlesFont);
		titleBox.getChildren().add(title);
		
		Button downloadButton = new Button("Begin Download");
		HBox downloadButtonBox = new HBox();
		downloadButtonBox.setPadding(new Insets(0, 10, 0, 0));
		downloadButtonBox.setAlignment(Pos.CENTER_RIGHT);
		downloadButtonBox.getChildren().add(downloadButton);
		// Add handlers to the buttons
		downloadButton.setOnAction(new EventHandler<ActionEvent>() {
			public void handle(ActionEvent ae) {
				beginDownload();
			}
		});
		
		HBox detailsConsoleTitleBox = new HBox();
		detailsConsoleTitleBox.setAlignment(Pos.CENTER);
		detailsConsoleTitleBox.setPadding(new Insets(10, 0, 0, 0));
		
		Text detailsConsoleTitle = new Text("Details Console");
		detailsConsoleTitle.setFont(titlesFont);
		detailsConsoleTitleBox.getChildren().add(detailsConsoleTitle);	
		
		this.detailsConsole = new VBox();
		this.detailsConsole.setStyle("-fx-background-color: #000000;");
		detailsConsole.setPadding(new Insets(4, 0, 4, 4));	
		this.detailsConsole.setPrefHeight(200);
		
		this.consoleWrapper = new ScrollPane();
		this.consoleWrapper.setFitToWidth(true);
		this.consoleWrapper.setContent(detailsConsole);
		this.consoleWrapper.setVvalue(consoleWrapper.getVmin());
		this.detailsConsole.setPrefHeight(200);
		
		this.consoleWrapper.maxHeightProperty().addListener(new ChangeListener<Number>() {
            @Override
            public void changed(ObservableValue<? extends Number> observable, Number num1, Number num2) {
            	if(scrollToBottom || true)
            	{
            		consoleWrapper.setVvalue(consoleWrapper.getVmax());
            		scrollToBottom = false;
            	}
              }
          });
		
		
		detailsBox.getChildren().addAll(
				titleBox, 
				createDetailsPane(), 
				downloadButtonBox,
				detailsConsoleTitleBox,
				consoleWrapper);		
		
		return detailsBox;
	}
	
	private GridPane createDetailsPane()
	{
		GridPane detailsPane = new GridPane();
		detailsPane.setHgap(10);
		detailsPane.setVgap(10);
	    detailsPane.setPadding(new Insets(0, 10, 0, 10));
	    
	    
	    Font titlesFont = Font.font("Calibri", FontWeight.BOLD, 12);
	    Font valuesFont = Font.font("Calibri", FontWeight.NORMAL, 12);
	    
	    Text fn = new Text("File Name:");
	    fn.setFont(titlesFont);	    
	    fileNameText = new Text("N/A");
	    fileNameText.setFont(valuesFont);
	    Text ih = new Text("Info Hash:");
	    ih.setFont(titlesFont);
	    infoHashText = new Text("N/A");
	    infoHashText.setFont(valuesFont);
	    Text np = new Text("Total Pieces:");
	    np.setFont(titlesFont);
	    numberOfPiecesText = new Text("0");
	    numberOfPiecesText.setFont(valuesFont);
	    Text dp = new Text("Downloaded Pieces:");
	    dp.setFont(titlesFont);
	    downloadedPiecesText = new Text("0");
	    downloadedPiecesText.setFont(valuesFont);	    
	    Text ds = new Text("Download Speed:");
	    ds.setFont(titlesFont);
	    downloadSpeedText = new Text();
	    downloadSpeedText.setFont(valuesFont);
	    setDownloadSpeedInternal(0);
	    Text npr = new Text("Peers Number:");
	    npr.setFont(titlesFont);
	    peersNumberText = new Text("0");
	    peersNumberText.setFont(valuesFont);
	    
	    detailsPane.add(fn, 0, 0);
	    detailsPane.add(fileNameText, 1, 0);
	    detailsPane.add(ih, 0, 1);
	    detailsPane.add(infoHashText, 1, 1);	    
	    detailsPane.add(np, 0, 2);
	    detailsPane.add(numberOfPiecesText, 1, 2);	    
	    detailsPane.add(dp, 0, 3);
	    detailsPane.add(downloadedPiecesText, 1, 3);	    
	    detailsPane.add(ds, 0, 4);
	    detailsPane.add(downloadSpeedText, 1, 4);	    
	    detailsPane.add(npr, 0, 5);
	    detailsPane.add(peersNumberText, 1, 5);    
	    
	    return detailsPane;
	}
	
	private FlowPane createFileOpenPane()
	{
		FlowPane openPane = new FlowPane(10, 10);
		
		openPane.setPadding(new Insets(8, 8, 8, 8));
		openPane.setAlignment(Pos.CENTER_LEFT);
		openPane.setStyle("-fx-background-color: #336699;");
		addControls(openPane.getChildren());
		
		return openPane;
	}
	
	private void openTorrent()
	{
		FileChooser fileChooser = new FileChooser();
		fileChooser.setTitle("Open Torrent File");
		fileChooser.getExtensionFilters().add(
			new FileChooser.ExtensionFilter("Torrent Files", "*.*")
		);
		
		File ret = fileChooser.showOpenDialog(null);
		if(ret != null && ret.exists())
		{
			printConsoleInfoInternal("Opened file: " + ret.getAbsolutePath());
		}
		else
		{
			printConsoleInfoInternal("No file chosen.");
		}
	}
	private void beginDownload()
	{
		printConsoleInfoInternal("Starting download.");
		//setStatusBarTextInternal("Starting download...");
	}
	
	private void setStatusBarTextInternal(String text)
	{
		this.statusBarText.setText(text);			
	}
	private void setFileNameInternal(String text)
	{
		this.fileNameText.setText(text);
	}	
	private void setInfoHashInternal(String text)
	{
		this.infoHashText.setText(text);
	}
	private void setNumberOfPiecesInternal(String text)
	{
		this.numberOfPiecesText.setText(text);
	}
	private void setDownloadedPiecesInternal(String text)
	{
		this.downloadedPiecesText.setText(text);
	}
	private void setDownloadSpeedInternal(int bps)
	{
		this.downloadSpeedText.setText(bps + " bps");
	}
	private void setPeersNumberInternal(String text)
	{
		this.peersNumberText.setText(text);
	}
	private void printConsoleInfoInternal(String text)
	{
		Text outputText = new Text(text);
		outputText.setFill(Color.WHITE);
		
		detailsConsole.getChildren().add(outputText);
		
		Platform.runLater(new Runnable() {

			@Override
			public void run() {
				consoleWrapper.setVvalue(consoleWrapper.getVmax());
			}
		});	
	}
	
	

	
	
}
