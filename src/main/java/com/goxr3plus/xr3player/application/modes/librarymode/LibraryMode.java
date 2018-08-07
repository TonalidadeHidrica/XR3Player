package main.java.com.goxr3plus.xr3player.application.modes.librarymode;

import java.io.File;
import java.io.IOException;
import java.sql.PreparedStatement;
import java.sql.Statement;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.Properties;
import java.util.logging.Level;

import org.atteo.evo.inflector.English;

import com.jfoenix.controls.JFXButton;

import javafx.application.Platform;
import javafx.beans.InvalidationListener;
import javafx.beans.Observable;
import javafx.beans.binding.Bindings;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.StringProperty;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Bounds;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ColorPicker;
import javafx.scene.control.Label;
import javafx.scene.control.MenuButton;
import javafx.scene.control.MenuItem;
import javafx.scene.control.ScrollBar;
import javafx.scene.control.SplitPane;
import javafx.scene.image.Image;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.util.Duration;
import main.java.com.goxr3plus.xr3player.application.Main;
import main.java.com.goxr3plus.xr3player.application.modes.librarymode.Library.LibraryStatus;
import main.java.com.goxr3plus.xr3player.application.presenter.SearchBox;
import main.java.com.goxr3plus.xr3player.application.presenter.SearchBox.SearchBoxType;
import main.java.com.goxr3plus.xr3player.application.presenter.Viewer;
import main.java.com.goxr3plus.xr3player.application.tools.ActionTool;
import main.java.com.goxr3plus.xr3player.application.tools.InfoTool;
import main.java.com.goxr3plus.xr3player.application.tools.JavaFXTools;
import main.java.com.goxr3plus.xr3player.application.tools.NotificationType;
import main.java.com.goxr3plus.xr3player.smartcontroller.media.FileCategory;
import main.java.com.goxr3plus.xr3player.smartcontroller.presenter.SmartController;

/**
 * This class contains everything needed going on LibraryMode.
 *
 * @author SuperGoliath
 */
public class LibraryMode extends BorderPane {
	
	// ------------------------------------------------
	
	@FXML
	private SplitPane topSplitPane;
	
	@FXML
	private StackPane bottomStackPane;
	
	@FXML
	private SplitPane bottomSplitPane;
	
	@FXML
	private StackPane noLibrariesStackPane;
	
	@FXML
	private StackPane librariesStackView;
	
	@FXML
	private ScrollBar horizontalScrollBar;
	
	@FXML
	private Label quickSearchTextField;
	
	@FXML
	private HBox libraryToolBar;
	
	@FXML
	private Button deleteLibrary;
	
	@FXML
	private Button renameLibrary;
	
	@FXML
	private Button openOrCloseLibrary;
	
	@FXML
	private JFXButton previous;
	
	@FXML
	private MenuButton createLibraryMenuButton;
	
	@FXML
	private MenuItem createLibrary;
	
	@FXML
	private MenuItem createAndOpenLibrary;
	
	@FXML
	private MenuItem crAndOpenFromFolder;
	
	@FXML
	private MenuItem crAndOpenFromFiles;
	
	@FXML
	private JFXButton next;
	
	@FXML
	private Button openLibraryContextMenu;
	
	@FXML
	private ColorPicker colorPicker;
	
	@FXML
	private HBox botttomHBox;
	
	@FXML
	private CheckBox multipleSelection;
	
	@FXML
	private Label librariesInfoLabel;
	
	@FXML
	private Button createFirstLibrary;
	
	@FXML
	private StackPane djModeStackPane;
	
	// ------------------------------------------------
	
	// protected boolean dragDetected
	
	/**
	 * The mechanism which allows you to transport items between libraries and more.
	 */
	public final SearchBox librariesSearcher = new SearchBox(SearchBoxType.LIBRARYSEARCHBOX);
	
	/**
	 * The mechanism which allows you to view the libraries as components with image etc.
	 */
	public Viewer viewer;
	
	/** The mechanism behind of opening multiple libraries. */
	public final OpenedLibrariesViewer openedLibrariesViewer = new OpenedLibrariesViewer();
	
	//--------Images ------------------------------
	
	/**
	 * Default image of a library(which has not a costume one selected by the user.
	 */
	public static Image defaultImage = InfoTool.getImageFromResourcesFolder("playlistImage.png");
	
	private boolean openLibraryAfterCreation;
	private List<File> createLibraryFromFiles;
	
	//----- Library Specific ------------------
	
	/** A PopUp window showing information about the selected library */
	public LibraryInformation libraryInformation = new LibraryInformation();
	
	/** The context menu. */
	public LibraryContextMenu librariesContextMenu = new LibraryContextMenu();
	
	/**
	 * This binding contains a number which shows how many libraries are currently opened
	 */
	public SimpleIntegerProperty openedLibraries = new SimpleIntegerProperty();
	
	/**
	 * This binding contains a number which shows how many libraries have currently no items at all
	 */
	public SimpleIntegerProperty emptyLibraries = new SimpleIntegerProperty();
	
	//----- Invalidation Listeners ------------------
	
	/** This variable is used during the creation of a new library. */
	private final InvalidationListener creationInvalidator = new InvalidationListener() {
		@Override
		public void invalidated(Observable observable) {
			
			// Remove the Listener
			Main.renameWindow.showingProperty().removeListener(this);
			
			// !Showing && !XPressed
			if (!Main.renameWindow.isShowing() && Main.renameWindow.wasAccepted()) {
				
				Main.window.requestFocus();
				
				// Check if this name already exists
				String name = Main.renameWindow.getUserInput();
				
				// if can pass
				if (!viewer.getItemsObservableList().stream().anyMatch(lib -> ( (Library) lib ).getLibraryName().equals(name))) {
					String tableName;
					boolean validName;
					
					// Until the randomName doesn't already exists
					do {
						tableName = ActionTool.returnRandomTableName();
						validName = !Main.dbManager.doesTableExist(tableName);
					} while (!validName);
					final String dataBaseTableName = tableName; //add it to a final variable
					
					//Ok Now Go
					try (PreparedStatement insertNewLibrary = Main.dbManager.getConnection()
							.prepareStatement("INSERT INTO LIBRARIES (NAME,TABLENAME,STARS,DATECREATED,TIMECREATED,DESCRIPTION,SAVEMODE,POSITION,LIBRARYIMAGE,OPENED) "
									+ "VALUES (?,?,?,?,?,?,?,?,?,?)");
							Statement statement = Main.dbManager.getConnection().createStatement()) {
						
						// Create the dataBase table
						statement.executeUpdate("CREATE TABLE '" + dataBaseTableName + "' (PATH       TEXT    PRIMARY KEY   NOT NULL ," + "STARS       DOUBLE     NOT NULL,"
								+ "TIMESPLAYED  INT     NOT NULL," + "DATE        TEXT   	NOT NULL," + "HOUR        TEXT    NOT NULL)");
						
						// Create the Library
						Library currentLib = new Library(name, dataBaseTableName, 0, null, null, null, 1, viewer.getItemsObservableList().size(), null, false);
						
						// Add the library
						//currentLib.goOnSelectionMode(selectionModeToggle.isSelected())
						viewer.addItem(currentLib, true);
						
						// Add a row on libraries table
						insertNewLibrary.setString(1, name);
						insertNewLibrary.setString(2, dataBaseTableName);
						insertNewLibrary.setDouble(3, currentLib.starsProperty().get());
						insertNewLibrary.setString(4, currentLib.getDateCreated());
						insertNewLibrary.setString(5, currentLib.getTimeCreated());
						insertNewLibrary.setString(6, currentLib.getDescription());
						insertNewLibrary.setInt(7, 1);
						insertNewLibrary.setInt(8, currentLib.getPosition());
						insertNewLibrary.setString(9, null);
						insertNewLibrary.setBoolean(10, false);
						
						insertNewLibrary.executeUpdate();
						
						// Commit
						Main.dbManager.commit();
						
						//Recalculate Some Bindings
						calculateEmptyLibraries();
						
						//Check if the user wants to immediately open library after it's creation
						if (openLibraryAfterCreation) {
							currentLib.setLibraryStatus(LibraryStatus.OPENED, false);
							
						}
						
						//Bidirectional binding with Instant Search
						currentLib.getSmartController().getInstantSearch().selectedProperty()
								.bindBidirectional(Main.settingsWindow.getPlayListsSettingsController().getInstantSearch().selectedProperty());
						
						//Fix maximum per playlist
						currentLib.getSmartController().setNewMaximumPerPage(Main.settingsWindow.getPlayListsSettingsController().getMaximumPerPlaylist(), false);
						currentLib.getSmartController().getReloadVBox().setVisible(false);
						
						//Check if directly create library from Files
						if (createLibraryFromFiles != null) {
							currentLib.getSmartController().getInputService().start(createLibraryFromFiles);
							createLibraryFromFiles = null;
						}
						
						//Check if the user wants to immediately open library after it's creation
						if (openLibraryAfterCreation)
							openedLibrariesViewer.selectTab(currentLib.getLibraryName());
						
					} catch (Exception ex) {
						Main.logger.log(Level.WARNING, "", ex);
						ActionTool.showNotification("Error Creating a Library", "Library can't be created cause of:" + ex.getMessage(), Duration.seconds(2),
								NotificationType.WARNING);
					}
				} else {
					ActionTool.showNotification("Dublicate Name", "A Library or PlayList with this name already exists!", Duration.seconds(2), NotificationType.INFORMATION);
				}
			}
			
			//Disable the openLibrary when the user creates a new Library
			if (!Main.renameWindow.isShowing())
				openLibraryAfterCreation = false;
			
		}
	};
	
	/**
	 * Constructor.
	 */
	public LibraryMode() {
		
		// FXMLLOADER
		FXMLLoader loader = new FXMLLoader(getClass().getResource(InfoTool.LIBRARIES_FXMLS + "LibraryMode.fxml"));
		loader.setController(this);
		loader.setRoot(this);
		
		try {
			loader.load();
		} catch (IOException ex) {
			ex.printStackTrace();
		}
		
	}
	
	/**
	 * Return the library with the given name.
	 *
	 * @param name
	 *            the name
	 * @return the library with name
	 */
	public Optional<Library> getLibraryWithName(String name) {
		
		// Find that
		for (Node library : viewer.getItemsObservableList())
			if ( ( (Library) library ).getLibraryName().equals(name))
				return Optional.of((Library) library);
			
		return Optional.ofNullable(null);
	}
	
	/**
	 * Called as soon as FXML file has been loaded
	 */
	@FXML
	public void initialize() {
		
		//Initialize
		viewer = new Viewer(this, horizontalScrollBar);
		quickSearchTextField.visibleProperty().bind(viewer.searchWordProperty().isEmpty().not());
		quickSearchTextField.textProperty().bind(Bindings.concat("Search :> ").concat(viewer.searchWordProperty()));
		
		// createLibrary
		createLibrary.setOnAction(a -> createNewLibrary(createLibraryMenuButton, false));
		
		//createAndOpenLibrary
		createAndOpenLibrary.setOnAction(a -> createNewLibrary(createLibraryMenuButton, true));
		
		//crAndOpenFromFolder
		crAndOpenFromFolder.setOnAction(a -> createNewLibraryFromFolder(createLibraryMenuButton, true, true));
		
		//crAndOpenFromFiles
		crAndOpenFromFiles.setOnAction(a -> createNewLibraryFromFolder(createLibraryMenuButton, false, false));
		
		// newLibrary
		createFirstLibrary.setOnAction(a -> createNewLibrary(createFirstLibrary.getGraphic(), true, true));
		createFirstLibrary.visibleProperty().bind(Bindings.size(viewer.getItemsObservableList()).isEqualTo(0));
		
		// selectionModeToggle
		//selectionModeToggle.selectedProperty().addListener((observable , oldValue , newValue) -> teamViewer.goOnSelectionMode(newValue))
		
		// searchLibrary
		botttomHBox.getChildren().add(librariesSearcher);
		
		// previous
		previous.setOnAction(a -> viewer.previous());
		
		// next
		next.setOnAction(a -> viewer.next());
		
		//showSettings
		//showSettings.setOnAction(a -> Main.settingsWindow.showWindow(SettingsTab.LIBRARIES))
		
		// StackPane
		librariesStackView.getChildren().addAll(viewer, librariesSearcher.region, librariesSearcher.searchProgress);
		viewer.toBack();
		
		// -- openLibrariesContextMenu
		openLibraryContextMenu.setOnAction(a -> {
			Library library = (Library) viewer.getSelectedItem();
			Bounds bounds = library.localToScreen(library.getBoundsInLocal());
			librariesContextMenu.show(Main.window, bounds.getMinX() + bounds.getWidth() / 3, bounds.getMinY() + bounds.getHeight() / 4, library);
		});
		
		// -- libraryToolBar
		libraryToolBar.disableProperty().bind(viewer.centerItemProperty().isNull());
		
		// -- renameLibrary
		renameLibrary.setOnAction(a -> ( (Library) viewer.centerItemProperty().get() ).renameLibrary(renameLibrary));
		
		// -- deleteLibrary
		deleteLibrary.setOnAction(a -> ( (Library) viewer.centerItemProperty().get() ).deleteLibrary(deleteLibrary));
		
		// -- openOrCloseLibrary 
		viewer.centerItemProperty().addListener((observable , oldValue , newValue) -> {
			if (newValue != null)
				openOrCloseLibrary.textProperty().bind(Bindings.when( ( (Library) viewer.centerItemProperty().get() ).openedProperty()).then("CLOSE").otherwise("OPEN"));
			else {
				StringProperty p1;
				p1 = openOrCloseLibrary.textProperty();
				p1.unbind();
				openOrCloseLibrary.setText("...");
			}
		});
		
		BooleanProperty p1 = openOrCloseLibrary.disableProperty();
		p1.bind(libraryToolBar.disabledProperty());
		openOrCloseLibrary.setOnAction(l -> ( (Library) viewer.centerItemProperty().get() )
				.setLibraryStatus( ( (Library) viewer.centerItemProperty().get() ).isOpened() ? LibraryStatus.CLOSED : LibraryStatus.OPENED, false));
		
		// -- settingsOfLibrary
		//openLibraryInformation.setOnAction(a -> libraryInformation.showWindow(teamViewer.centerItemProperty().get()));
		
		// -- goToLibraryPlayList
		//		goToLibraryPlayList.setOnAction(a -> Optional.ofNullable(teamViewer.centerItemProperty().get()).ifPresent(library -> {
		//			if (library.isOpened())
		//				multipleLibs.selectTab(library.getLibraryName());
		//		}));
		
		//----librariesInfoLabel
		librariesInfoLabel.textProperty()
				.bind(Bindings.createStringBinding(
						() -> "[ " + viewer.itemsWrapperProperty().sizeProperty().get() + " ] " + English.plural("Library", viewer.itemsWrapperProperty().sizeProperty().get())
								+ " , [ " + openedLibraries.get() + " ] Opened , [ " + emptyLibraries.get() + " ] Empty",
						viewer.itemsWrapperProperty().sizeProperty(), openedLibraries, emptyLibraries));
		
		//== colorPicker
		String defaultWebColor = "#ef4949";
		colorPicker.setValue(Color.web(defaultWebColor));
		viewer.setStyle("-fx-background-color: linear-gradient(to bottom,transparent 60,#141414 60.2%, " + defaultWebColor + " 87%);");
		colorPicker.setOnAction(λ -> Main.dbManager.getPropertiesDb().updateProperty("Libraries-Background-Color", JavaFXTools.colorToWebColor(colorPicker.getValue())));
		colorPicker.valueProperty().addListener((observable , oldColor , newColor) -> viewer
				.setStyle("-fx-background-color: linear-gradient(to bottom,transparent 60,#141414 60.2%, " + JavaFXTools.colorToWebColor(newColor) + "  87%);"));
		
		//bottomSplitPane
		bottomSplitPane.visibleProperty().bind(djModeStackPane.visibleProperty().not());
		
		//multipleSelection
		multipleSelection.selectedProperty().addListener(l -> {
			boolean selected = multipleSelection.isSelected();
			
			//For each library
			viewer.getItemsObservableList().forEach(library -> ( (Library) library ).goOnSelectionMode(selected));
		});
		
	}
	
	/**
	 * Recalculates the opened libraries
	 */
	public void calculateOpenedLibraries() {
		openedLibraries.set((int) viewer.getItemsObservableList().stream().filter(library -> ( (Library) library ).isOpened()).count());
	}
	
	/**
	 * Recalculates the empty libraries
	 */
	public void calculateEmptyLibraries() {
		emptyLibraries.set((int) viewer.getItemsObservableList().stream().filter(library -> ( (Library) library ).isEmpty()).count());
	}
	
	/**
	 * Used to create a new Library
	 * 
	 * @param owner
	 */
	public void createNewLibrary(Node owner , boolean openLibraryAfterCreation , boolean... exactPositioning) {
		this.openLibraryAfterCreation = openLibraryAfterCreation;
		
		// Open rename window
		Main.renameWindow.show("", owner, "Create " + ( !openLibraryAfterCreation ? "" : "+ Open " ) + "new Library", FileCategory.DIRECTORY, exactPositioning);
		
		// Add the showing listener
		Main.renameWindow.showingProperty().addListener(creationInvalidator);
	}
	
	/**
	 * Create a new library from folder
	 * 
	 * @param owner
	 * @param openLibraryAfterCreation
	 * @param files
	 */
	public void createNewLibraryFromFolder(Node owner , boolean openLibraryAfterCreation , boolean importJustOneFolder) {
		
		//Import just a folder
		if (importJustOneFolder) {
			File file = Main.specialChooser.selectFolder(Main.window);
			if (file != null)
				this.createLibraryFromFiles = Arrays.asList(file);
			else
				return;
		} else { //Import many files
			List<File> list = Main.specialChooser.prepareToImportSongFiles(Main.window);
			if (list != null && !list.isEmpty())
				this.createLibraryFromFiles = list;
			else
				return;
			
		}
		
		//Call the original method
		createNewLibrary(owner, openLibraryAfterCreation);
	}
	
	/**
	 * Loads all [ Opened-Libraries ] and the [ Last-Opened-Library ] as properties from the UserInformation.properties file [[SuppressWarningsSpartan]]
	 */
	public void loadOpenedLibraries() {
		
		//Get the current User
		Main.dbManager.getOpenedUser().ifPresent(user -> {
			
			//Load the properties
			Properties properties = user.getUserInformationDb().loadProperties();
			
			//Load the opened libraries
			//			Optional.ofNullable(properties.getProperty("Opened-Libraries")).ifPresent(openedLibraries -> {
			//				
			//				//Use the split to get all the Opened Libraries Names
			//				Arrays.asList(openedLibraries.split("\\<\\|\\>\\:\\<\\|\\>")).stream().forEach(name -> {
			//					Platform.runLater(() -> {
			//						//System.out.println(name); //debugging
			//						
			//						//Get the Library and Open it!
			//						getLibraryWithName(name).get().libraryOpenClose(true, true);
			//					});
			//				});
			//			});
			
			//Load all the Opened Libraries
			Platform.runLater(() -> viewer.getItemsObservableList().stream().filter(library -> ( (Library) library ).isOpened())
					.forEach(library -> ( (Library) library ).setLibraryStatus(LibraryStatus.OPENED, true)));
			
			//Add Selection Model ChangeListener 
			Platform.runLater(() -> {
				
				//Library Mode Tab Pane Selection Listener
				openedLibrariesViewer.getTabPane().getSelectionModel().selectedItemProperty().addListener((observable , oldTab , newTab) -> {
					
					// Give refresh based on the below formula
					Optional.ofNullable(newTab).ifPresent(tab -> {
						SmartController smartController = ( (SmartController) tab.getContent() );
						
						//Check 
						if ( ( smartController.isFree(false) && smartController.getItemsObservableList().isEmpty() ) || smartController.getReloadVBox().isVisible()) {
							
							//Just a trick when i create a library directly from Folder or Files
							if (createLibraryFromFiles != null)
								//Refresh the SmartController
								smartController.getLoadService().startService(false, true, true);
							
							//Store the Opened Libraries
							//storeOpenedLibraries()		
						}
						
						//System.out.println("Changed...")
						storeLastOpenedLibrary();
					});
				});
				
				//Emotion Lists Tab Pane Selection Listener
				Main.emotionsTabPane.getTabPane().getSelectionModel().selectedItemProperty().addListener((observable , oldTab , newTab) -> {
					
					// Give refresh based on the below formula
					SmartController smartController = ( (SmartController) newTab.getContent() );
					if ( ( !openedLibrariesViewer.getTabPane().getTabs().isEmpty() && smartController.isFree(false) && smartController.getItemsObservableList().isEmpty() )
							|| smartController.getReloadVBox().isVisible()) {
						
						( (SmartController) newTab.getContent() ).getLoadService().startService(false, true, true);
						
					}
				});
				
				//Load the Last Opened Library
				Optional.ofNullable(properties.getProperty("Last-Opened-Library")).ifPresent(lastOpenedLibrary -> {
					
					//Select the correct library inside the TabPane
					openedLibrariesViewer.getTabPane().getSelectionModel().select(openedLibrariesViewer.getTab(lastOpenedLibrary));
					
					//This will change in future update when user can change the default position of Libraries
					viewer.setCenterIndex(openedLibrariesViewer.getSelectedLibrary().get().getPosition());
					
				});
				
				//Update last selected Library SmartController if not empty
				openedLibrariesViewer.getSelectedLibrary().ifPresent(selectedLibrary -> {
					if (selectedLibrary.getSmartController().isFree(false))
						selectedLibrary.getSmartController().getLoadService().startService(false, true, false);
				});
			});
		});
		
	}
	
	/**
	 * Stores the last opened library - That means the library that was selected on the Multiple Libraries Tab Pane <br>
	 * !Must be called from JavaFX Thread!
	 */
	public void storeLastOpenedLibrary() {
		
		//Get the current User
		Main.dbManager.getOpenedUser().ifPresent(user -> {
			
			//Save the last opened(selected) library if any
			if (openedLibrariesViewer.getTabs().isEmpty())
				user.getUserInformationDb().deleteProperty("Last-Opened-Library");
			else
				user.getUserInformationDb().updateProperty("Last-Opened-Library", openedLibrariesViewer.getTabPane().getSelectionModel().getSelectedItem().getTooltip().getText());
			
		});
	}
	
	/**
	 * Stores all the opened libraries and the last selected one as properties to the UserInformation.properties file <br>
	 * !Must be called from JavaFX Thread!
	 * 
	 * @param openedLibrariesTabs
	 */
	public void storeOpenedLibraries() {
		
		//Get the opened user and store the opened libraries
		//		getOpenedUser().ifPresent(user -> {
		//			ObservableList<Tab> openedLibrariesTabs = openedLibrariesViewer.getTabs();
		//			
		//			//			//Save the opened libraries
		//			//			if (openedLibrariesTabs.isEmpty())
		//			//				user.getUserInformationDb().deleteProperty("Opened-Libraries");
		//			//			else {
		//			//				
		//			//				//Join all library names to a string using as separator char "<|>:<|>"
		//			//				String openedLibs = openedLibrariesTabs.stream().map(tab -> tab.getTooltip().getText()).collect(Collectors.joining("<|>:<|>"));
		//			//				user.getUserInformationDb().updateProperty("Opened-Libraries", openedLibs);
		//			//				
		//			//				//System.out.println("Opened Libraries:\n-> " + openedLibs); //debugging
		//			//			}
		//			
		//			//Save the last opened library
		//			storeLastOpenedLibrary();
		//		});
		storeLastOpenedLibrary();
	}
	
	/**
	 * Gets the previous.
	 *
	 * @return the previous
	 */
	public Button getPrevious() {
		return previous;
	}
	
	/**
	 * Gets the next.
	 *
	 * @return the next
	 */
	public Button getNext() {
		return next;
	}
	
	/**
	 * @return the horizontalScrollBar
	 */
	public ScrollBar getHorizontalScrollBar() {
		return horizontalScrollBar;
	}
	
	//	// Variables
	//	private double[] topSplitPaneDivider = { 0.45 , 0.55 };
	//	
	//	// Variables
	//	private double[] bottomSplitPaneDivider = { 0.6 , 0.4 };
	
	//	/**
	//	 * Updates the values of array that holds DividerPositions of splitPane
	//	 */
	//	public void updateTopSplitPaneDividerArray(double[] array) {
	//		topSplitPaneDivider[0] = array[0];
	//		topSplitPaneDivider[1] = array[1];
	//	}
	//	
	//	/**
	//	 * Updates the values of array that holds DividerPositions of splitPane
	//	 */
	//	public void updateBottomSplitPaneDividerArray(double[] array) {
	//		bottomSplitPaneDivider[0] = array[0];
	//		bottomSplitPaneDivider[1] = array[1];
	//	}
	
	//----------------------------
	
	//	/**
	//	 * Updates the SplitPane DividerPositions based on the saved array
	//	 */
	//	public void updateTopSplitPaneDivider() {
	//		topSplitPane.setDividerPositions(topSplitPaneDivider);
	//	}
	//	
	//	/**
	//	 * Updates the SplitPane DividerPositions based on the saved array
	//	 */
	//	public void updateBottomSplitPaneDivider() {
	//		bottomSplitPane.setDividerPositions(bottomSplitPaneDivider);
	//	}
	//	
	//	//----------------------------	
	//	
	//	/**
	//	 * Saves current divider positions of SplitPane into an array
	//	 */
	//	public void saveTopSplitPaneDivider() {
	//		topSplitPaneDivider = topSplitPane.getDividerPositions();
	//	}
	//	
	//	/**
	//	 * Saves current divider positions of SplitPane into an array
	//	 */
	//	public void saveBottomSplitPaneDivider() {
	//		bottomSplitPaneDivider = bottomSplitPane.getDividerPositions();
	//	}
	//	
	//	/**
	//	 * Turns the Library Mode Upside Down or opposite
	//	 * 
	//	 * @param turnDown
	//	 */
	//	public void turnUpsideDownSplitPane(boolean turnDown) {
	//		
	//		//Check if it can enter based on the library border pane position
	//		if ( ( turnDown && !topSplitPane.getItems().get(0).equals(Main.playListModesSplitPane) )
	//				|| ( !turnDown && topSplitPane.getItems().get(0).equals(Main.playListModesSplitPane) ))
	//			return;
	//		
	//		//this.saveTopSplitPaneDivider()
	//		double temp = topSplitPaneDivider[0];
	//		topSplitPaneDivider[0] = topSplitPaneDivider[1];
	//		topSplitPaneDivider[1] = temp;
	//		
	//		boolean libraryIsOnTop = topSplitPane.getItems().get(0).equals(Main.playListModesSplitPane);
	//		topSplitPane.getItems().clear();
	//		if (libraryIsOnTop) {
	//			//System.out.println("Entered first if!")
	//			topSplitPane.getItems().addAll(bottomSplitPane, Main.playListModesSplitPane);
	//		} else {
	//			//System.out.println("Entered second if!")
	//			topSplitPane.getItems().addAll(Main.playListModesSplitPane, bottomSplitPane);
	//		}
	//		
	//		this.updateTopSplitPaneDivider();
	//		
	//	}
	
	//----------------------------
	
	/**
	 * @return the topSplitPane
	 */
	public SplitPane getTopSplitPane() {
		return topSplitPane;
	}
	
	/**
	 * @return the bottomSplitPane
	 */
	public SplitPane getBottomSplitPane() {
		return bottomSplitPane;
	}
	
	/**
	 * @return the colorPicker
	 */
	public ColorPicker getColorPicker() {
		return colorPicker;
	}
	
	/**
	 * @return the noLibrariesStackPane
	 */
	public StackPane getNoLibrariesStackPane() {
		return noLibrariesStackPane;
	}
	
	/**
	 * @return the djModeStackPane
	 */
	public StackPane getDjModeStackPane() {
		return djModeStackPane;
	}
	
	/**
	 * @return the bottomStackPane
	 */
	public StackPane getBottomStackPane() {
		return bottomStackPane;
	}
	
	/**
	 * @return the multipleSelection
	 */
	public CheckBox getMultipleSelection() {
		return multipleSelection;
	}
	
}
