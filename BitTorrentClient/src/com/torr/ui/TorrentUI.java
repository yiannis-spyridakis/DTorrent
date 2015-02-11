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
import javafx.scene.input.Clipboard;
import javafx.scene.input.ClipboardContent;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.control.*;
import javafx.scene.text.*;
import jfx.messagebox.MessageBox;

import java.util.concurrent.*;

import com.torr.client.*;


public class TorrentUI extends Application implements ITorrentUI {
	
	private final int CONSOLE_LINES_NUM_LOW = 100;
	private final int CONSOLE_LINES_NUM_HIGH = 150;
	
	private Text statusBarText = null;
	private Text workspaceFolderText = null;
	private Text fileNameText = null;
	private Text infoHashText = null;
	private Text numberOfPiecesText = null;
	private Text downloadedPiecesText = null;
	private Text downloadSpeedText = null;
	private Text peersNumberText = null;
	private Text saveLocationText = null;
	private ScrollPane consoleWrapper = null;
	private VBox detailsConsole = null;
	private boolean scrollToBottom = false;
	private TorrentMain torrentMain = null;
	private Stage mainStage = null;
	private String saveLocation = null;
	
	public static void main(String[] args) 
	{
		
		System.out.println("Launching JavaFX UI");		
		
		launch(args);

	}	
	
	@Override
	public void init() 
	{	
		System.out.println("Initializing the user interface...");
	}
	
	@Override
	public void start(Stage stage) 
	{
		
		mainStage = stage;
		
		try
		{			
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
			Scene myScene = new Scene(screenPane, 720, 520);
			
			// Set the scene on the stage
			stage.setScene(myScene);
			
			// Connect UI with DTorrent
			this.torrentMain = new TorrentMain(this);
			
			// Window title
			stage.setTitle(String.format(Consts.MAIN_WINDOW_NAME, torrentMain.GetPeerId()));
			
			
			// Show the stage and its scene.
			stage.show();				
		
		}
		catch(Exception ex)
		{
			this.Quit(ex);
		}
		
	}
	
	
	@Override
	public void stop() 
	{		
		System.out.println("UI shutting down...");
		
		if(this.torrentMain != null)
		{
			this.torrentMain.close();
		}
		System.exit(0);
	}
	
	/*
	 * 
	 * Thread safe methods 
	 * 
	 */		
	
	@Override
	public FutureTask<Integer> ShowMessageBox(final String message, final String title, final int options)
	{
		MessageBoxWrapper msb = new MessageBoxWrapper(message, title, options);
		
		Platform.runLater(msb);
		return msb.GetResult();
	}
	
	@Override
	public void Quit()
	{
        Platform.runLater(new Runnable() {

            @Override
            public void run() {
            	Platform.exit();
            }
        });					
	}
	@Override
	public void Quit(Exception ex)
	{
		try
		{
			// This statement blocks until the message box returns
			this.ShowMessageBox(
					"Unable to continue running application:\n" + ex.getMessage(), 
					"Unable to continue running application", MessageBox.ICON_ERROR).get();
		}
		catch(Exception ex_inner)
		{	
			ex_inner.printStackTrace();
		}
		
		this.Quit();		
	}
	
	@Override
	public void SetStatusBarText(final String text)
	{
        Platform.runLater(new Runnable() {

            @Override
            public void run() {
            	setStatusBarTextInternal(text);
            }
        });				
	}
	@Override
	public void SetFileName(final String text)
	{
        Platform.runLater(new Runnable() {

            @Override
            public void run() {
            	setFileNameInternal(text);
            }
        });		
	}	
	@Override
	public void SetInfoHash(final String text)
	{
        Platform.runLater(new Runnable() {

            @Override
            public void run() {
            	setInfoHashInternal(text);
            }
        });	
	}
	@Override
	public void SetNumberOfPieces(final String text)
	{
        Platform.runLater(new Runnable() {

            @Override
            public void run() {
            	setNumberOfPiecesInternal(text);
            }
        });	
	}
	@Override
	public void SetDownloadedPieces(final String text)
	{
        Platform.runLater(new Runnable() {

            @Override
            public void run() {
            	setDownloadedPiecesInternal(text);
            }
        });	
	}
	@Override
	public void SetDownloadSpeed(final int bps)
	{
        Platform.runLater(new Runnable() {
        	
            @Override
            public void run() {
            	
            	setDownloadSpeedInternal(bps);
            }
        });	
	}
	@Override
	public void SetPeersNumber(final String text)
	{
        Platform.runLater(new Runnable() {

            @Override
            public void run() {
            	setPeersNumberInternal(text);
            }
        });	
	}	
	
	@Override
	public void SetSaveLocation(final String text)
	{
        Platform.runLater(new Runnable() {

            @Override
            public void run() {
            	SetSaveLocationInternal(text);
            }
        });			
	}
	
	@Override
	public void PrintConsoleInfo(final String text)
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

		
		HBox detailsConsoleTitleBox = new HBox();
		detailsConsoleTitleBox.setAlignment(Pos.CENTER);
		detailsConsoleTitleBox.setPadding(new Insets(10, 0, 0, 0));
		
		Text detailsConsoleTitle = new Text("Information Console");
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
		this.consoleWrapper.setPrefHeight(200);
		
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
	    Text npr = new Text("Connected Peers:");
	    npr.setFont(titlesFont);
	    peersNumberText = new Text("0");
	    peersNumberText.setFont(valuesFont);
	    Text sl = new Text("Save location:");
	    sl.setFont(titlesFont);
	    saveLocationText = new Text("N/A");
	    saveLocationText.setFont(valuesFont);
	    Button copyButton = new Button("Copy");
	    
	    
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
	    detailsPane.add(sl, 0, 6);
	    detailsPane.add(saveLocationText, 1, 6);
	    detailsPane.add(copyButton, 2, 6);
	    
	    copyButton.setOnAction(new EventHandler<ActionEvent>()
	    {
	    	public void handle(ActionEvent ae)
	    	{
	    		final ClipboardContent content = new ClipboardContent();
	    		content.putString(TorrentUI.this.saveLocation);
	    		Clipboard.getSystemClipboard().setContent(content);
	    	}
	    });
	    
	    
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
		try
		{
			FileChooser fileChooser = new FileChooser();
			fileChooser.setTitle("Open Torrent File");
			fileChooser.getExtensionFilters().add(
				new FileChooser.ExtensionFilter("Torrent Files", "*.*")
			);
			
			File ret = fileChooser.showOpenDialog(null);
			if(ret != null && ret.exists())
			{
				final String filePath = ret.getAbsolutePath();
				printConsoleInfoInternal("Opening torrent file: " + filePath);
				torrentMain.OpenTorrentFile(filePath);
			}
			else
			{
				printConsoleInfoInternal("No file chosen.");
			}
		}
		catch(Exception ex)
		{
			printConsoleInfoInternal("Unable to open torrent file:");
			printConsoleInfoInternal(ex.getMessage());
		}
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
	private void SetSaveLocationInternal(final String text)
	{
		// Cache for clipboard copying
		saveLocation = text;
		
		String finalText = text;
		if(finalText.length() > 60)
		{
			finalText = finalText.substring(0, 60) + "...";
		}		
		
		this.saveLocationText.setText(finalText);
	}
	
	private void printConsoleInfoInternal(String text)
	{
		Text outputText = new Text(text);
		outputText.setFill(Color.WHITE);
		
		ObservableList<Node> children = detailsConsole.getChildren();
		
		// Drop lines num to 100 if they're over 150
		int size = children.size();
		if(size > CONSOLE_LINES_NUM_HIGH)
		{
			children.remove(0, size - CONSOLE_LINES_NUM_LOW);
		}
		
		children.add(outputText);
		
		Platform.runLater(new Runnable() {

			@Override
			public void run() {
				consoleWrapper.setVvalue(consoleWrapper.getVmax());
			}
		});	
	}
	
	class MessageBoxWrapper implements Callable<Integer>, Runnable
	{
		private int msgBoxResult = 0;
		private String message = null;
		private String title = null;
		private int options = 0;
		private FutureTask<Integer> result = null;
		
		public MessageBoxWrapper(String message, String title, int options)
		{
			this.message = message;
			this.title = title;
			this.options = options;
			result = new FutureTask<Integer>(this);
		}
		
		public FutureTask<Integer> GetResult()
		{
			return this.result;
		}
		
		@Override 
		public Integer call()
		{
			return msgBoxResult;
		}
		@Override
		public void run()
		{
			msgBoxResult = MessageBox.show(null, this.message, this.title, this.options);
			result.run();
		}
	}	
	
}




