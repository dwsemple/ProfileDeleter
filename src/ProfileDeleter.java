
import java.awt.event.ActionListener;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.TimeZone;
import java.util.regex.Pattern;

/**
 * Class with functionality to delete user profile folders from a computer
 * running Windows.
 * <p>
 * In Windows 7 and up when a user logs into a computer it stores a copy of
 * their profile in the (default) folder C:\Users.<br>
 * It also adds some data to the registry for the user in the registry keys:
 * HKEY_LOCAL_MACHINE\\SOFTWARE\\Microsoft\\Windows
 * NT\\CurrentVersion\\ProfileList.<br>
 * HKEY_LOCAL_MACHINE\\SOFTWARE\\Microsoft\\Windows
 * NT\\CurrentVersion\\ProfileGuid.
 * <p>
 * To remove a user account from a computer in Windows, whether to conserve hard
 * drive space or because the users profile is corrupt on the computer you need
 * to:<br>
 * <ol>
 * <li>Delete the users folder from the users directory.</li>
 * <li>Match the registry key in ProfileList to the user account and delete the
 * key
 * under ProfileList.</li>
 * <li>Match the registry key in ProfileGuid to the users registry key in
 * ProfileList and delete the matching key under ProfileGuid.</li>
 * </ol>
 * <p>
 * Doing this for more than one user at a time can be tedious and time
 * consuming.<br>
 * This class automates this process.
 * <p>
 * Can set an ActionListener on the class if creating a GUI that displays the
 * ProfileDeleter's log. ProfileDeleter will trigger a "LogWritten" ActionEvent
 * on the ActionListener to notify the ActionListener that the log has been
 * updated.
 */
public class ProfileDeleter {

    /**
     * Class attributes.
     */
    private String computer;
    private String users_directory;
    private String remote_data_directory;
    private String local_data_directory;
    private List<UserData> user_list;
    private List<String> cannot_delete_list;
    private List<String> log_list;
    private String session_id;
    private boolean size_check;
    private boolean state_check;
    private boolean registry_check;
    private boolean size_check_complete;
    private boolean state_check_complete;
    private boolean registry_check_complete;
    private ActionListener log_updated;

    /**
     * Severity level for logged messages.
     */
    public enum LOG_TYPE {
        INFO(0),
        WARNING(1),
        ERROR(2);

        private int state;

        LOG_TYPE(int new_state) {
            state = new_state;
        }

        public int GetState() {
            return state;
        }
    }

    /**
     * Constructor for ProfileDeleter class.
     */
    public ProfileDeleter() {
        this(null);
    }

    /**
     * Constructor for ProfileDeleter class that allows an ActionListener to be
     * specified.
     * <p>
     * When the log list attribute is updated ProfileDeleter will trigger a
     * "LogWritten" ActionEvent on the ActionListener to notify it that the log
     * has been updated.<br>
     * This is intended to allow GUI classes to update any UI elements that
     * display the log.
     *
     * @param log_updated the ActonListener to notify that the log has been
     * updated
     */
    public ProfileDeleter(ActionListener log_updated) {
        computer = "";
        users_directory = "";
        remote_data_directory = "";
        local_data_directory = "";
        user_list = new ArrayList<>();
        log_list = new ArrayList<>();
        cannot_delete_list = new ArrayList<>();
        cannot_delete_list.add("Public");
        session_id = "";
        size_check = false;
        state_check = true;
        registry_check = true;
        size_check_complete = false;
        state_check_complete = false;
        registry_check_complete = false;
        this.log_updated = log_updated;
    }

    /**
     * Sets the computer and users directory attributes.
     *
     * @param computer the hostname or IP address of the target computer
     */
    public void setComputer(String computer) {
        this.computer = computer;
        this.users_directory = "\\\\" + computer + "\\c$\\users\\";
        logMessage("Remote computer set to " + computer, LOG_TYPE.INFO, true);
    }

    /**
     * Sets the value of the users directory attribute.
     *
     * @param users_directory the filepath to the target computers users
     * directory
     */
    public void setUsersDirectory(String users_directory) {
        this.users_directory = users_directory;
        logMessage("Users directory set to " + users_directory, LOG_TYPE.INFO, true);
    }

    /**
     * Sets the value of the remote data directory attribute
     *
     * @param remote_data_directory the filepath to the directory on the target
     * computer for storing ProfileDeleter data
     */
    public void setRemoteDataDirectory(String remote_data_directory) {
        this.remote_data_directory = remote_data_directory;
        logMessage("Remote data directory set to " + remote_data_directory, LOG_TYPE.INFO, true);
    }

    /**
     * Sets the value of the local data directory attribute
     *
     * @param local_data_directory the filepath to the directory on the local
     * computer for storing ProfileDeleter data
     */
    public void setLocalDataDirectory(String local_data_directory) {
        this.local_data_directory = local_data_directory;
        logMessage("Local data directory set to " + local_data_directory, LOG_TYPE.INFO, true);
    }

    /**
     * Sets the size check attribute.
     * <p>
     * Determines whether a size check is done when checks are run.
     *
     * @param size_check whether to run a size check or not
     */
    public void setSizeCheck(boolean size_check) {
        this.size_check = size_check;
        logMessage("Size check set to " + size_check, LOG_TYPE.INFO, true);
    }

    /**
     * Sets the state check attribute.
     * <p>
     * Determines whether a state check is done when checks are run.
     *
     * @param state_check whether to run a state check or not
     */
    public void setStateCheck(boolean state_check) {
        this.state_check = state_check;
        logMessage("State check set to " + state_check, LOG_TYPE.INFO, true);
    }

    /**
     * Sets the registry check attribute.
     * <p>
     * Determines whether a registry check is done when checks are run.
     *
     * @param registry_check whether to run a registry check or not
     */
    public void setRegistryCheck(boolean registry_check) {
        this.registry_check = registry_check;
        logMessage("Registry check set to " + registry_check, LOG_TYPE.INFO, true);
    }

    /**
     * Sets the size check complete attribute.
     * <p>
     * Whether a size check has been completed or not.
     *
     * @param size_check Whether a size check has been completed or not
     */
    public void setSizeCheckComplete(boolean size_check) {
        this.size_check_complete = size_check_complete;
        logMessage("Size check complete set to " + size_check_complete, LOG_TYPE.INFO, true);
    }

    /**
     * Sets the state check complete attribute.
     * <p>
     * Whether a state check has been completed or not.
     *
     * @param state_check Whether a state check has been completed or not
     */
    public void setStateCheckComplete(boolean state_check) {
        this.state_check_complete = state_check_complete;
        logMessage("State check complete set to " + state_check_complete, LOG_TYPE.INFO, true);
    }

    /**
     * Sets the registry check complete attribute.
     * <p>
     * Whether a registry check has been completed or not.
     *
     * @param registry_check Whether a registry check has been completed or not
     */
    public void setRegistryCheckComplete(boolean registry_check_complete) {
        this.registry_check_complete = registry_check_complete;
        logMessage("Registry check complete set to " + registry_check_complete, LOG_TYPE.INFO, true);
    }

    /**
     * Sets the user list attribute.
     *
     * @param user_list list of UserData that contain details for the users on
     * the target computer
     */
    public void setUserList(List<UserData> user_list) {
        this.user_list = user_list;
    }

    /**
     * Sets the cannot delete list attribute.
     *
     * @param cannot_delete_list list of user account names that are not allowed
     * to be deleted
     */
    public void setCannotDeleteList(List<String> cannot_delete_list) {
        this.cannot_delete_list = cannot_delete_list;
    }

    /**
     * Sets the log list attribute.
     *
     * @param log_list list of logged events
     */
    public void setLogList(List<String> log_list) {
        this.log_list = log_list;
    }
    
    /**
     * Sets the log updated attribute.
     *
     * @param log_updated the ActionListener to notify when the log is updated
     */
    public void setLogUpdatedActionListener(ActionListener log_updated) {
        this.log_updated = log_updated;
    }

    /**
     * Gets the value of the computer attribute.
     *
     * @return the target computer hostname or IP address
     */
    public String getComputer() {
        return computer;
    }

    /**
     * Gets the value of the users directory attribute.
     *
     * @return the filepath to the target computers users directory
     */
    public String getUsersDirectory() {
        return users_directory;
    }

    /**
     * Gets the value of the remote data directory attribute
     *
     * @return the filepath to the directory on the target computer for storing
     * ProfileDeleter data
     */
    public String getRemoteDataDirectory() {
        return remote_data_directory;
    }

    /**
     * Gets the value of the local data directory attribute
     *
     * @return the filepath to the directory on the local computer for storing
     * ProfileDeleter data
     */
    public String getLocalDataDirectory() {
        return local_data_directory;
    }

    /**
     * Gets the size check attribute.
     *
     * @return whether to run a size check or not
     */
    public boolean getSizeCheck() {
        return size_check;
    }

    /**
     * Gets the state check attribute.
     *
     * @return whether to run a state check or not
     */
    public boolean getStateCheck() {
        return state_check;
    }

    /**
     * Gets the registry check attribute.
     *
     * @return whether to run a registry check or not
     */
    public boolean getRegistryCheck() {
        return registry_check;
    }

    /**
     * Gets the size check complete attribute.
     *
     * @return whether a size check has been completed
     */
    public boolean getSizeCheckComplete() {
        return size_check_complete;
    }

    /**
     * Gets the state check complete attribute.
     *
     * @return whether a state check has been completed
     */
    public boolean getStateCheckComplete() {
        return state_check_complete;
    }

    /**
     * Gets the registry check complete attribute.
     *
     * @return whether a registry check has been completed
     */
    public boolean getRegistryCheckComplete() {
        return registry_check_complete;
    }

    /**
     * Gets the user list attribute.
     *
     * @return list of UserData that contain details for the users on the target
     * computer
     */
    public List<UserData> getUserList() {
        return user_list;
    }

    /**
     * Gets the cannot delete list attribute.
     *
     * @return list of user account names that are not allowed to be deleted
     */
    public List<String> getCannotDeleteList() {
        return cannot_delete_list;
    }

    /**
     * Gets the log list attribute.
     *
     * @return list of logged events
     */
    public List<String> getLogList() {
        return log_list;
    }
    
    /**
     * Gets the log updated attribute.
     *
     * @return the ActionListener to notify when the log is updated
     */
    public ActionListener getLogUpdatedActionListener() {
        return log_updated;
    }

    /**
     * Converts the user list attribute into a 2D Object array so it can be
     * displayed in a JTable.
     *
     * @return 2D Object array of the user list attribute
     */
    public Object[][] convertUserListTo2DObjectArray() {
        Object[][] object_array = new Object[user_list.size()][];
        for (int i = 0; i < user_list.size(); i++) {
            object_array[i] = user_list.get(i).toObjectArray();
        }
        return object_array;
    }

    /**
     * Process local Windows account deletion on target computer.
     * <p>
     * Before this can be done the user list attribute must be set and state
     * check + registry check must be set to true.<br>
     * These can be done manually or by running the generateUserList function to
     * create the user list and running the checking functions checkState,
     * checkRegistry or checkAll.<br>
     * A size check does not need to be done to process the deletion, this is an
     * optional check as it can take a very long time to complete.
     *
     * @return deletion deletion report detailing users deleted and any problems
     * deleting the user folder or registry keys
     * @throws NotInitialisedException user list has not been initialised or a
     * state and/or registry check has not been run
     */
    public List<String> processDeletion() throws NotInitialisedException {
        logMessage("Attempting to run deletion on users list", LOG_TYPE.INFO, true);
        if (user_list != null && !user_list.isEmpty() && state_check_complete && registry_check_complete) {
            ArrayList<UserData> new_folders = new ArrayList<UserData>();
            ArrayList<String> deleted_folders = new ArrayList<String>();
            deleted_folders.add("User" + '\t' + "Folder Deleted?" + '\t' + "Registry SID Deleted?" + '\t' + "Registry GUID Deleted?");
            for (UserData user : user_list) {
                if (user.getDelete()) {
                    logMessage("User " + user.getName() + " is flagged for deletion", LOG_TYPE.INFO, true);
                    String deleted_user = user.getName() + '\t';
                    try {
                        directoryDelete(users_directory + user.getName());
                        deleted_user += "Yes" + '\t';
                        logMessage("Successfully deleted user directory for " + user.getName(), LOG_TYPE.INFO, true);
                    } catch (IOException | CannotEditException e) {
                        String message = "Failed to delete user directory " + user.getName() + ". Error is " + e.getMessage();
                        deleted_user += message + '\t';
                        logMessage(message, LOG_TYPE.ERROR, true);
                    }
                    try {
                        if (user.getSid().compareTo("") != 0) {
                            registryDelete(computer, "HKEY_LOCAL_MACHINE\\SOFTWARE\\Microsoft\\Windows NT\\CurrentVersion\\ProfileList\\" + user.getSid());
                            deleted_user += "Yes " + user.getSid() + '\t';
                            logMessage("Successfully deleted SID " + user.getSid() + " for user " + user.getName(), LOG_TYPE.INFO, true);
                        } else {
                            deleted_user += "SID is blank" + '\t';
                            logMessage("SID for user " + user.getName() + " is blank", LOG_TYPE.WARNING, true);
                        }
                    } catch (IOException | InterruptedException e) {
                        String message = "Failed to delete user SID " + user.getSid() + " from registry. Error is " + e.getMessage();
                        deleted_user += message + '\t';
                        logMessage(message, LOG_TYPE.ERROR, true);
                    }
                    try {
                        if (user.getGuid().compareTo("") != 0) {
                            registryDelete(computer, "HKEY_LOCAL_MACHINE\\SOFTWARE\\Microsoft\\Windows NT\\CurrentVersion\\ProfileGuid\\" + user.getGuid());
                            deleted_user += "Yes " + user.getGuid();
                            logMessage("Successfully deleted GUID " + user.getGuid() + " for user " + user.getName(), LOG_TYPE.INFO, true);
                        } else {
                            deleted_user += "GUID is blank";
                            logMessage("GUID for user " + user.getName() + " is blank", LOG_TYPE.WARNING, true);
                        }
                    } catch (IOException | InterruptedException e) {
                        String message = "Failed to delete user GUID " + user.getGuid() + " from registry. Error is " + e.getMessage();
                        deleted_user += message;
                        logMessage(message, LOG_TYPE.ERROR, true);
                    }
                    deleted_folders.add(deleted_user);
                } else {
                    new_folders.add(user);
                }
            }
            user_list = new_folders;
            logMessage("Successfully completed deletions", LOG_TYPE.INFO, true);
            return deleted_folders;
        } else {
            String message = "Either user list has not been initialised or a state and/or registry check has not been run";
            logMessage(message, LOG_TYPE.WARNING, true);
            throw new NotInitialisedException(message);
        }
    }

    /**
     * Backs up ProfileList and ProfileGuid registry keys on the target computer
     * and copies the files to the local computer.
     * <p>
     * Local data directory and remote data directory attributes need to be
     * initialised before this function can be run.<br>
     * ProfileList registry key =
     * HKEY_LOCAL_MACHINE\\SOFTWARE\\Microsoft\\Windows
     * NT\\CurrentVersion\\ProfileList.<br>
     * ProfileGuid registry key =
     * HKEY_LOCAL_MACHINE\\SOFTWARE\\Microsoft\\Windows
     * NT\\CurrentVersion\\ProfileGuid.
     *
     * @throws IOException thrown if any IO errors are received running registry
     * backup and file copy commands on target and local computer
     * @throws InterruptedException thrown if any of the process threads used to
     * run registry backup and file copy commands on the target and local
     * computer become interrupted
     * @throws CannotEditException unable to create required folders or files on
     * target or local computer
     * @throws NotInitialisedException local or remote data directory attribute
     * has not been initialised
     */
    public void backupAndCopyRegistry() throws IOException, InterruptedException, CannotEditException, NotInitialisedException {
        logMessage("Attempting to backup profilelist and profileguid registry keys on remote computer", LOG_TYPE.INFO, true);
        if (local_data_directory.compareTo("") == 0 || remote_data_directory.compareTo("") == 0) {
            String message = "Local or remote data directory has not been initialised";
            logMessage(message, LOG_TYPE.WARNING, true);
            throw new NotInitialisedException(message);
        } else {
            String filename_friendly_computer = computer.replace('.', '_');
            int count = 0;
            boolean run = true;
            while (run) {
                try {
                    registryBackup(computer, "HKEY_LOCAL_MACHINE\\SOFTWARE\\Microsoft\\Windows NT\\CurrentVersion\\ProfileList", "C:\\temp\\Profile_Deleter\\" + session_id + "\\" + filename_friendly_computer + "_ProfileList.reg");
                    run = false;
                } catch (IOException | InterruptedException | CannotEditException e) {
                    if (count > 29) {
                        throw e;
                    } else {
                        logMessage("Attempt " + Integer.toString(count + 1) + " at backing up registry key failed", LOG_TYPE.WARNING, true);
                        count++;
                    }
                }
            }
            run = true;
            count = 0;
            while (run) {
                try {
                    registryBackup(computer, "HKEY_LOCAL_MACHINE\\SOFTWARE\\Microsoft\\Windows NT\\CurrentVersion\\ProfileGuid", "C:\\temp\\Profile_Deleter\\" + session_id + "\\" + filename_friendly_computer + "_ProfileGuid.reg");
                    run = false;
                } catch (IOException | InterruptedException | CannotEditException e) {
                    if (count > 29) {
                        throw e;
                    } else {
                        logMessage("Attempt " + Integer.toString(count + 1) + " at backing up registry key failed", LOG_TYPE.WARNING, true);
                        count++;
                    }
                }
            }
            try {
                fileCopy(remote_data_directory + "\\" + filename_friendly_computer + "_ProfileList.reg", local_data_directory);
            } catch (IOException | CannotEditException e) {
                throw e;
            }
            try {
                fileCopy(remote_data_directory + "\\" + filename_friendly_computer + "_ProfileGuid.reg", local_data_directory);
            } catch (IOException | CannotEditException e) {
                throw e;
            }
        }
    }

    /**
     * Processes ProfileSid and ProfileGuid registry data obtained from target
     * computer and assigns the values to the correct user in the user list
     * attribute.
     * <p>
     * backupAndCopyRegistry function must be run before this function can be
     * run, or the needed .reg files need to be manually created.<br>
     * Local data directory attribute must be initialised before this function
     * can be run.
     *
     * @throws IOException thrown if IO errors are received when trying to open
     * needed .reg files
     * @throws NotInitialisedException local data directory attribute has not
     * been initialised
     */
    public void findSIDAndGUID() throws IOException, NotInitialisedException {
        logMessage("Attempting to compile SID and GUID data from registry backups", LOG_TYPE.INFO, true);
        if (local_data_directory.compareTo("") != 0) {
            List<String> regkeys_profile_list;
            List<String> regkeys_profile_guid;
            String filename_friendly_computer = computer.replace('.', '_');
            try {
                logMessage("Loading file " + local_data_directory + "\\" + filename_friendly_computer + "_ProfileList.reg", LOG_TYPE.INFO, true);
                regkeys_profile_list = readFromFile(local_data_directory + "\\" + filename_friendly_computer + "_ProfileList.reg");
                logMessage("Loading file " + local_data_directory + "\\" + filename_friendly_computer + "_ProfileGuid.reg", LOG_TYPE.INFO, true);
                regkeys_profile_guid = readFromFile(local_data_directory + "\\" + filename_friendly_computer + "_ProfileGuid.reg");
                if (regkeys_profile_list != null && !regkeys_profile_list.isEmpty() && regkeys_profile_guid != null && !regkeys_profile_guid.isEmpty()) {
                    logMessage("Cleaning data from file " + local_data_directory + "\\" + filename_friendly_computer + "_ProfileList.reg", LOG_TYPE.INFO, true);
                    List<String> cleaned_regkeys_profile_list = new ArrayList<String>();
                    List<String> cleaned_regkeys_profile_guid = new ArrayList<String>();
                    for (int i = 0; i < regkeys_profile_list.size(); i++) {
                        if ((i % 2) == 0) {
                            String cleaned_string = "";
                            for (int j = 0; j < regkeys_profile_list.get(i).length(); j++) {
                                if ((j % 2) != 0) {
                                    cleaned_string += regkeys_profile_list.get(i).charAt(j);
                                }
                            }
                            cleaned_regkeys_profile_list.add(cleaned_string);
                        }
                    }
                    regkeys_profile_list = cleaned_regkeys_profile_list;
                    logMessage("Cleaning data from file " + local_data_directory + "\\" + filename_friendly_computer + "_ProfileGuid.reg", LOG_TYPE.INFO, true);
                    for (int i = 0; i < regkeys_profile_guid.size(); i++) {
                        if ((i % 2) == 0) {
                            String cleaned_string = "";
                            for (int j = 0; j < regkeys_profile_guid.get(i).length(); j++) {
                                if ((j % 2) != 0) {
                                    cleaned_string += regkeys_profile_guid.get(i).charAt(j);
                                }
                            }
                            cleaned_regkeys_profile_guid.add(cleaned_string);
                        }
                    }
                    regkeys_profile_guid = cleaned_regkeys_profile_guid;
                    String current_sid = "";
                    String profile_path = "";
                    String profile_guid = "";
                    boolean found_profile_path = false;
                    int count = 0;
                    logMessage("Processing file " + local_data_directory + "\\" + filename_friendly_computer + "_ProfileList.reg", LOG_TYPE.INFO, true);
                    for (String line : regkeys_profile_list) {
                        if (line.startsWith("[HKEY_LOCAL_MACHINE\\SOFTWARE\\Microsoft\\Windows NT\\CurrentVersion\\ProfileList\\") || count == regkeys_profile_list.size() - 1) {
                            String new_sid = line.replace("[HKEY_LOCAL_MACHINE\\SOFTWARE\\Microsoft\\Windows NT\\CurrentVersion\\ProfileList\\", "");
                            new_sid = new_sid.replaceAll("]", "");
                            new_sid = new_sid.trim();
                            logMessage("Found new SID " + new_sid, LOG_TYPE.INFO, true);
                            if (!profile_path.isEmpty()) {
                                logMessage("Processing details for found profile " + profile_path, LOG_TYPE.INFO, true);
                                boolean found_user = false;
                                for (UserData user : user_list) {
                                    if (user.getName().compareTo(profile_path) == 0) {
                                        found_user = true;
                                        logMessage("Found matching user account", LOG_TYPE.INFO, true);
                                        if (!user.getSid().isEmpty()) {
                                            logMessage("SID already exists for user, resolving conflict", LOG_TYPE.INFO, true);
                                            boolean found_guid = false;
                                            for (String guid : regkeys_profile_guid) {
                                                if (guid.contains("[HKEY_LOCAL_MACHINE\\SOFTWARE\\Microsoft\\Windows NT\\CurrentVersion\\ProfileGuid\\")) {
                                                    String trimmed_guid = guid.replace("[HKEY_LOCAL_MACHINE\\SOFTWARE\\Microsoft\\Windows NT\\CurrentVersion\\ProfileGuid\\", "");
                                                    trimmed_guid = trimmed_guid.replaceAll("]", "");
                                                    if (trimmed_guid.compareTo(profile_guid) == 0) {
                                                        logMessage("Found matching GUID from " + local_data_directory + "\\" + filename_friendly_computer + "_ProfileGuid.reg. Checking SID for match", LOG_TYPE.INFO, true);
                                                        found_guid = true;
                                                    }
                                                } else if (found_guid) {
                                                    String sid = guid.replace("\"SidString\"=\"", "");
                                                    sid = sid.replaceAll("\"", "");
                                                    if (sid.compareTo(current_sid) == 0) {
                                                        logMessage("New SID details match SID details for GUID, replacing user details with new details. SID set to " + current_sid + " and GUID to " + profile_guid, LOG_TYPE.INFO, true);
                                                        user_list.get(user_list.indexOf(user)).setSid(current_sid);
                                                        user_list.get(user_list.indexOf(user)).setGuid(profile_guid);
                                                        break;
                                                    }
                                                }
                                            }
                                            logMessage("No match found, discarding new details found", LOG_TYPE.INFO, true);
                                        } else {
                                            logMessage("Set SID for user " + profile_path + " to " + current_sid + " and GUID to " + profile_guid, LOG_TYPE.INFO, true);
                                            user_list.get(user_list.indexOf(user)).setSid(current_sid);
                                            user_list.get(user_list.indexOf(user)).setGuid(profile_guid);
                                        }
                                        break;
                                    }
                                }
                                if (!found_user) {
                                    logMessage("No matching user found for profile " + profile_path, LOG_TYPE.INFO, true);
                                }
                                current_sid = new_sid;
                                profile_path = "";
                                profile_guid = "";
                            } else {
                                current_sid = new_sid;
                                logMessage("SID is " + current_sid, LOG_TYPE.INFO, true);
                            }
                        } else if (line.startsWith("\"ProfileImagePath\"")) {
                            logMessage("User directory exists in SID, processing details", LOG_TYPE.INFO, true);
                            profile_path = line;
                            found_profile_path = true;
                        } else if (found_profile_path) {
                            if (line.startsWith("  ")) {
                                profile_path += line;
                            } else {
                                profile_path = profile_path.replace("\"ProfileImagePath\"=hex(2):", "");
                                profile_path = profile_path.replaceAll("00,", "");
                                profile_path = profile_path.replaceAll("\\\\", "");
                                profile_path = profile_path.replaceAll(" ", "");
                                profile_path = profile_path.replaceAll("\\n", "");
                                profile_path = profile_path.replaceAll("\\r", "");
                                String[] profile_path_hex = profile_path.split(",");
                                String profile_path_hex_to_string = "";
                                for (String hex : profile_path_hex) {
                                    int decimal = Integer.parseInt(hex, 16);
                                    profile_path_hex_to_string += (char) decimal;
                                }
                                profile_path = profile_path_hex_to_string.replace("C:\\Users\\", "");
                                profile_path = profile_path.trim();
                                found_profile_path = false;
                                logMessage("Found user directory " + profile_path, LOG_TYPE.INFO, true);
                            }
                        } else if (line.startsWith("\"Guid\"")) {
                            profile_guid = line.replace("\"Guid\"=\"", "");
                            profile_guid = profile_guid.replaceAll("\"", "");
                            profile_guid = profile_guid.trim();
                            logMessage("Found GUID " + profile_guid, LOG_TYPE.INFO, true);
                        }
                        count++;
                    }
                    registry_check_complete = true;
                    logMessage("Successfully compiled SID and GUID data from registry backups", LOG_TYPE.INFO, true);
                } else {
                    String message = "File " + local_data_directory + "\\" + filename_friendly_computer + "_ProfileList.reg or " + local_data_directory + "\\" + filename_friendly_computer + "_ProfileGuid.reg is either empty or corrupt";
                    logMessage(message, LOG_TYPE.ERROR, true);
                    throw new NotInitialisedException(message);
                }
            } catch (IOException e) {
                logMessage("Unable to read file " + local_data_directory + "\\" + filename_friendly_computer + "_ProfileList.reg. File may not exist. Error is " + e.getMessage(), LOG_TYPE.ERROR, true);
                throw e;
            }
        }
    }

    /**
     * Populates the user list attribute with data from the target computers
     * users directory.
     * <p>
     * Gets the folder name and last updated date for each user.<br>
     * Sets delete to true unless the folder name is in the cannot delete list.
     *
     * @throws IOException an IO error has occurred when running the powershell
     * script to get the user list on the target computer
     */
    public void generateUserList() throws IOException {
        logMessage("Attempting to build users directory " + users_directory, LOG_TYPE.INFO, true);
        if (users_directory.compareTo("") != 0) {
            try {
                user_list = new ArrayList<>();
                String command = "Set-ExecutionPolicy -ExecutionPolicy Bypass -Scope Process | powershell.exe -File \".\\src\\GetDirectoryList.ps1\" - directory " + users_directory;
                ProcessBuilder builder = new ProcessBuilder("C:\\Windows\\System32\\WindowsPowerShell\\v1.0\\powershell.exe", "-Command", command);
                builder.redirectErrorStream(true);
                Process power_shell_process = builder.start();
                BufferedReader powershell_process_output_stream = new BufferedReader(new InputStreamReader(power_shell_process.getInputStream()));
                String output = "";
                String line = "";
                while ((line = powershell_process_output_stream.readLine()).compareTo("EndOfScriptGetDirectoryList") != 0) {
                    if (!line.isEmpty()) {
                        logMessage("Discovered folder details " + line, LOG_TYPE.INFO, true);
                        String[] line_split = line.split("\\t");
                        UserData user = new UserData(true, line_split[0], line_split[1], "", "", "", "");
                        if (cannot_delete_list.contains(line_split[0])) {
                            user.setDelete(false);
                        }
                        user_list.add(user);
                    }
                }
                powershell_process_output_stream.close();
                power_shell_process.destroy();
                logMessage("Successfully built users directory " + users_directory, LOG_TYPE.INFO, true);
            } catch (IOException e) {
                logMessage("Failed to build users directory " + users_directory, LOG_TYPE.ERROR, true);
                logMessage(e.getMessage(), LOG_TYPE.ERROR, true);
                throw e;
            }
        } else {
            logMessage("Computer name has not been specified. Building users directory has been aborted", LOG_TYPE.WARNING, true);
        }
    }

    /**
     * Checks the size of each user folder on the target computer.
     * <p>
     * This check can take a very long time depending on the size of the users
     * directory on the target computer.<br>
     * This check is not required to run a deletion.
     */
    public void checkSize() {
        logMessage("Calcuting size of directory list", LOG_TYPE.INFO, true);
        if (user_list.size() > 0 && users_directory.compareTo("") != 0) {
            for (int i = 0; i < user_list.size(); i++) {
                String folder = user_list.get(i).getName();
                String folder_size = "";
                try {
                    folder_size = findFolderSize(folder);
                    logMessage("Calculated size " + folder_size + " for folder " + folder, LOG_TYPE.INFO, true);
                } catch (NonNumericException | IOException e) {
                    folder_size = "Could not calculate size";
                    logMessage(folder_size + " for folder " + folder, LOG_TYPE.WARNING, true);
                    logMessage(e.getMessage(), LOG_TYPE.ERROR, true);
                }
                user_list.get(i).setSize(folder_size);
            }
            size_check_complete = true;
            logMessage("Finished calculating size of directory list", LOG_TYPE.INFO, true);
        } else {
            logMessage("Directory list is empty, aborting size calculation", LOG_TYPE.WARNING, true);
        }
    }

    /**
     * Checks the state of each user folder on the target computer to determine
     * if the folder can be edited and therefore deleted.
     * <p>
     * This check is required before a deletion can be run.
     *
     * @throws IOException an IO error occurs when trying to check the editable
     * state of users in user list attribute
     */
    public void checkState() throws IOException {
        logMessage("Checking editable state of directory list", LOG_TYPE.INFO, true);
        if (user_list.size() > 0 && users_directory.compareTo("") != 0) {
            for (int i = 0; i < user_list.size(); i++) {
                String user = user_list.get(i).getName();
                logMessage("Checking editable state of folder " + user, LOG_TYPE.INFO, true);
                try {
                    if (!cannot_delete_list.contains(user)) {
                        directoryRename(computer, "C:\\users\\", user, user);
                        user_list.get(i).setState("Editable");
                        user_list.get(i).setDelete(true);
                    } else {
                        user_list.get(i).setState("Uneditable");
                        user_list.get(i).setDelete(false);
                        logMessage("User is in the cannot delete list, skipping check for this user", LOG_TYPE.INFO, true);
                    }
                } catch (CannotEditException e) {
                    String message = "Uneditable. User may be logged in or PC may need to be restarted";
                    logMessage(message, LOG_TYPE.WARNING, true);
                    user_list.get(i).setState(message);
                    user_list.get(i).setDelete(false);
                } catch (IOException e) {
                    logMessage("Editable state check has failed", LOG_TYPE.ERROR, true);
                    logMessage(e.getMessage(), LOG_TYPE.ERROR, true);
                    throw e;
                }
            }
            state_check_complete = true;
            logMessage("Finished checking editable state of directory list", LOG_TYPE.INFO, true);
        } else {
            logMessage("Directory list is empty, aborting editable state check", LOG_TYPE.WARNING, true);
        }
    }

    /**
     * Gets the registry sid and guid data for each user account on the target
     * computer.
     * <p>
     * This check is required before a deletion can be run.
     */
    public void checkRegistry() {
        logMessage("Getting registry SID and GUID values for user list", LOG_TYPE.INFO, true);
        generateSessionID();
        try {
            generateSessionFolders();
            try {
                backupAndCopyRegistry();
                try {
                    findSIDAndGUID();
                } catch (IOException | NotInitialisedException e) {
                    logMessage("Unable to process SID and GUID registry data, error is: " + e.getMessage(), LOG_TYPE.ERROR, true);
                }
            } catch (IOException | CannotEditException | NotInitialisedException | InterruptedException e) {
                logMessage("Unable to backup registry files, error is: " + e.getMessage(), LOG_TYPE.ERROR, true);
            }
        } catch (IOException | CannotEditException | NotInitialisedException e) {
            logMessage("Unable to create session folders, error is: " + e.getMessage(), LOG_TYPE.ERROR, true);
        }
    }

    /**
     * Runs the size check, state check and registry check if their
     * corresponding boolean attribute is set to true.
     * <p>
     * Set the corresponding boolean attribute for each check using the
     * setSizeCheck, setStateCheck and setRegistryCheck functions.
     *
     * @throws IOException an IO error occurs when trying to check the editable
     * state of users in user list attribute
     */
    public void checkAll() throws IOException {
        logMessage("Running all enabled checks", LOG_TYPE.INFO, true);
        if (size_check) {
            checkSize();
        } else {
            logMessage("Size check is turned off, skipping size check", LOG_TYPE.INFO, true);
        }
        if (state_check) {
            checkState();
        } else {
            logMessage("State check is turned off, skipping state check", LOG_TYPE.INFO, true);
        }
        if (registry_check) {
            checkRegistry();
        } else {
            logMessage("Registry check is turned off, skipping registry check", LOG_TYPE.INFO, true);
        }
        logMessage("Running enabled checks complete", LOG_TYPE.INFO, true);
    }

    /**
     * Generates a session ID for uniquely naming folders and files related to
     * the particular deletion.
     * <p>
     * The session ID is generated using the generateDateString function.
     */
    public void generateSessionID() {
        session_id = generateDateString();
        logMessage("Session ID has been set to " + session_id, LOG_TYPE.INFO, true);
    }

    /**
     * Creates the necessary folders for the deletion to run.
     * <p>
     * Creates a folder on the target computer in C:\temp\Profile_Deleter and a
     * folder on the local computer in the sessions folder.<br>
     * Session ID attribute must be set. The folders are named using the session
     * ID so that they are unique.<br>
     * Computer attribute must be set.
     *
     * @throws NotInitialisedException thrown in the session ID or computer
     * attribute are not set
     * @throws IOException an IO error occurs when trying to create the needed
     * folders
     * @throws CannotEditException unable to create the needed folders on the
     * target computer or the local computer
     */
    public void generateSessionFolders() throws NotInitialisedException, IOException, CannotEditException {
        logMessage("Attempting to create session user_list", LOG_TYPE.INFO, true);
        if (session_id.compareTo("") != 0 && computer.compareTo("") != 0) {
            try {
                directoryCreate("\\\\" + computer + "\\c$\\temp\\Profile_Deleter");
            } catch (IOException e) {
                throw e;
            } catch (CannotEditException e) {
            }
            try {
                directoryCreate("\\\\" + computer + "\\c$\\temp\\Profile_Deleter\\" + session_id);
                remote_data_directory = "\\\\" + computer + "\\c$\\temp\\Profile_Deleter\\" + session_id;
            } catch (IOException | CannotEditException e) {
                String message = "Unable to create remote data directory " + "\\\\" + computer + "\\c$\\temp\\Profile_Deleter\\" + session_id;
                logMessage(message, LOG_TYPE.ERROR, true);
                throw new CannotEditException(message);
            }
            try {
                directoryCreate(".\\sessions\\" + computer + "_" + session_id);
                local_data_directory = ".\\sessions\\" + computer + "_" + session_id;
            } catch (IOException | CannotEditException e) {
                String message = "Unable to create local data directory " + ".\\sessions\\" + computer + "_" + session_id;
                logMessage(message, LOG_TYPE.ERROR, true);
                throw new CannotEditException(message);
            }
            logMessage("Successfully created session user_list", LOG_TYPE.INFO, true);
        } else {
            String message = "";
            if (session_id.compareTo("") == 0) {
                message += "Session ID has not been created";
            }
            if (computer.compareTo("") == 0) {
                if (message.compareTo("") != 0) {
                    message += " and ";
                }
                message += "computer has not been initialised";
            }
            message += ". Please Initialise before running generateSessionFolders";
            logMessage(message, LOG_TYPE.ERROR, true);
            throw new NotInitialisedException(message);
        }
    }

    /**
     * Uses pstools to rename a folder.
     *
     * @param computer the computer the folder is on
     * @param directory the directory containing the folder to rename
     * @param folder the name of the folder to rename
     * @param folder_renamed the name to rename the folder to
     * @throws IOException an IO error occurs when trying to rename the folder
     * @throws CannotEditException unable to rename the folder
     */
    public void directoryRename(String computer, String directory, String folder, String folder_renamed) throws IOException, CannotEditException {
        try {
            logMessage("Attempting to rename folder " + directory + folder + " to " + folder_renamed, LOG_TYPE.INFO, true);
            String command = ".\\pstools\\psexec \\\\" + computer + " cmd /c REN \"" + directory + folder + "\" \"" + folder_renamed + "\" && echo editable|| echo uneditable";
            ProcessBuilder builder = new ProcessBuilder("C:\\Windows\\System32\\cmd.exe", "/c", command);
            builder.redirectErrorStream(true);
            Process cmd_process = builder.start();
            BufferedReader cmd_process_output_stream = new BufferedReader(new InputStreamReader(cmd_process.getInputStream()));
            String line = "";
            String error = "";
            while ((line = cmd_process_output_stream.readLine()) != null) {
                error = line;
            }
            if (error.compareTo("editable") != 0) {
                String message = "Unable to rename folder " + directory + folder + ". Error is: " + error;
                throw new CannotEditException(message);
            }
            logMessage("Successfully renamed folder " + directory + folder + " to " + folder_renamed, LOG_TYPE.INFO, true);
        } catch (CannotEditException | IOException e) {
            logMessage("Could not rename directory " + directory + folder, LOG_TYPE.WARNING, true);
            logMessage(e.getMessage(), LOG_TYPE.WARNING, true);
            throw e;
        }
    }

    /**
     * Calculates the filesize of a user folder on the target computer.
     * <p>
     * Uses powershell script GetFolderSize.ps1.
     *
     * @param user the name of the user folder to calculate the size of on the
     * target computer
     * @return the size of the folder
     * @throws NonNumericException the size calculated is not a number
     * @throws IOException an IO error has occurred when trying to calculate the
     * size of access and run the powershell script
     */
    public String findFolderSize(String user) throws NonNumericException, IOException {
        try {
            logMessage("Calculating filesize for folder " + users_directory + user, LOG_TYPE.INFO, true);
            String command = "Set-ExecutionPolicy -ExecutionPolicy Bypass -Scope Process | powershell.exe -File \".\\src\\GetFolderSize.ps1\" - directory " + users_directory + user;
            ProcessBuilder builder = new ProcessBuilder("C:\\Windows\\System32\\WindowsPowerShell\\v1.0\\powershell.exe", "-command", command);
            builder.redirectErrorStream(true);
            Process power_shell_process = builder.start();
            BufferedReader powershell_process_output_stream = new BufferedReader(new InputStreamReader(power_shell_process.getInputStream()));
            String output = "";
            String line = "";
            while ((line = powershell_process_output_stream.readLine()).compareTo("EndOfScriptGetFolderSize") != 0) {
                if (!line.isEmpty()) {
                    output = line;
                }
            }
            powershell_process_output_stream.close();
            power_shell_process.destroy();
            if (Pattern.matches("[0-9]+", output)) {
                logMessage("Successfully calculated filesize for folder " + users_directory + user + ": " + output, LOG_TYPE.INFO, true);
                return output;
            } else {
                String message = "Size calculated is not a number. Ensure powershell script .\\src\\GetFolderSize.ps1 is correct";
                logMessage(message, LOG_TYPE.ERROR, true);
                throw new NonNumericException(message);
            }
        } catch (NonNumericException | IOException e) {
            logMessage("Could not calculate size of folder " + users_directory + user, LOG_TYPE.ERROR, true);
            logMessage(e.getMessage(), LOG_TYPE.ERROR, true);
            throw e;
        }
    }

    /**
     * Creates a folder.
     * <p>
     * Can be used to create folders on remote computers using \\computername.
     *
     * @param directory the path + name of the folder to create
     * @throws IOException an IO error has occurred when running process to
     * create the folder
     * @throws CannotEditException unable to create the folder
     */
    public void directoryCreate(String directory) throws IOException, CannotEditException {
        try {
            logMessage("Attempting to create folder " + directory, LOG_TYPE.INFO, true);
            String command = "MKDIR \"" + directory + "\"";
            ProcessBuilder builder = new ProcessBuilder("C:\\Windows\\System32\\cmd.exe", "/c", command);
            builder.redirectErrorStream(true);
            Process cmd_process = builder.start();
            BufferedReader cmd_process_output_stream = new BufferedReader(new InputStreamReader(cmd_process.getInputStream()));
            String line = "";
            String error = "";
            while ((line = cmd_process_output_stream.readLine()) != null) {
                error = line;
            }
            if (error.compareTo("") != 0) {
                String message = "Folder " + directory + " already exists. Error is: " + error;
                logMessage(message, LOG_TYPE.WARNING, true);
                throw new CannotEditException(message);
            }
            logMessage("Successfully created folder " + directory, LOG_TYPE.INFO, true);
        } catch (IOException e) {
            logMessage("Could not create folder " + directory, LOG_TYPE.ERROR, true);
            logMessage(e.getMessage(), LOG_TYPE.ERROR, true);
            throw e;
        } catch (CannotEditException e) {
            throw e;
        }
    }

    /**
     * Deletes a folder.
     * <p>
     * Can be used to delete folders on remote computers using \\computername.
     *
     * @param directory the path + name of the folder to delete
     * @throws IOException an IO error has occurred when running process to
     * delete the folder
     * @throws CannotEditException unable to delete the folder
     */
    public void directoryDelete(String directory) throws IOException, CannotEditException {
        try {
            logMessage("Attempting to delete folder " + directory, LOG_TYPE.INFO, true);
            String command = "RMDIR /S /Q \"" + directory + "\"";
            ProcessBuilder builder = new ProcessBuilder("C:\\Windows\\System32\\cmd.exe", "/c", command);
            builder.redirectErrorStream(true);
            Process cmd_process = builder.start();
            BufferedReader cmd_process_output_stream = new BufferedReader(new InputStreamReader(cmd_process.getInputStream()));
            String line = "";
            String error = "";
            while ((line = cmd_process_output_stream.readLine()) != null) {
                error = line;
            }
            if (error.compareTo("") != 0) {
                String message = "Unable to delete folder " + directory + ". Error is: " + error;
                logMessage(message, LOG_TYPE.ERROR, true);
                throw new CannotEditException(message);
            }
            logMessage("Successfully deleted folder " + directory, LOG_TYPE.INFO, true);
        } catch (CannotEditException | IOException e) {
            logMessage("Could not delete folder " + directory, LOG_TYPE.ERROR, true);
            logMessage(e.getMessage(), LOG_TYPE.ERROR, true);
            throw e;
        }
    }

    /**
     * Deletes a list of files in a folder.
     * <p>
     * Can be used to delete files on remote computers using \\computername.
     *
     * @param directory the path + name of the folder to delete the files in
     * @param files the list of files to delete
     * @param do_not_delete files to not delete, can be used if the list of
     * files to delete is not filtered previously
     * @throws IOException an IO error occurred when attempting to delete the
     * files
     * @throws CannotEditException unable to delete files
     */
    public void directoryDeleteFiles(String directory, List<String> files, List<String> do_not_delete) throws IOException, CannotEditException {
        try {
            logMessage("Attempting to delete list of files in directory " + directory, LOG_TYPE.INFO, true);
            for (String file : files) {
                boolean delete = true;
                if (do_not_delete != null) {
                    for (String exclude_file : do_not_delete) {
                        if (file.compareTo(exclude_file) == 0) {
                            delete = false;
                        }
                    }
                }
                if (delete) {
                    String command = "del \"" + directory + "\\" + file + "\"";
                    ProcessBuilder builder = new ProcessBuilder("C:\\Windows\\System32\\cmd.exe", "/c", command);
                    builder.redirectErrorStream(true);
                    Process cmd_process = builder.start();
                    BufferedReader cmd_process_output_stream = new BufferedReader(new InputStreamReader(cmd_process.getInputStream()));
                    String line = "";
                    String error = "";
                    while ((line = cmd_process_output_stream.readLine()) != null) {
                        error = line;
                    }
                    if (error.compareTo("") != 0) {
                        String message = "Unable to delete file " + directory + "\\" + file + ". Error is: " + error;
                        logMessage(message, LOG_TYPE.ERROR, true);
                        throw new CannotEditException(message);
                    }
                    logMessage("Successfully deleted file " + directory + "\\" + file, LOG_TYPE.INFO, true);
                } else {
                    logMessage("File " + directory + "\\" + file + " is in do not delete list. It has not been deleted", LOG_TYPE.INFO, true);
                }
            }
        } catch (CannotEditException | IOException e) {
            logMessage("Failed to delete all requested files in directory " + directory, LOG_TYPE.ERROR, true);
            logMessage(e.getMessage(), LOG_TYPE.ERROR, true);
            throw e;
        }
    }

    /**
     * Gets a list of files in a folder
     *
     * @param directory the path + folder name of the folder to get the files
     * list from
     * @return the list of files in the designated folder
     * @throws IOException an IO error occurred when getting the list of files
     * @throws CannotEditException unable to read filenames from the designated
     * folder
     */
    public List<String> directoryListFiles(String directory) throws IOException, CannotEditException {
        try {
            logMessage("Attempting to get list of files in directory " + directory, LOG_TYPE.INFO, true);
            String command = "dir /b /a-d \"" + directory + "\"";
            ProcessBuilder builder = new ProcessBuilder("C:\\Windows\\System32\\cmd.exe", "/c", command);
            builder.redirectErrorStream(true);
            Process cmd_process = builder.start();
            BufferedReader cmd_process_output_stream = new BufferedReader(new InputStreamReader(cmd_process.getInputStream()));
            List<String> files = new ArrayList<String>();
            String line = "";
            String error = "";
            while ((line = cmd_process_output_stream.readLine()) != null) {
                if (line.compareTo("") != 0) {
                    files.add(line);
                }
                error = line;
            }
            if (error.compareTo("") != 0) {
                String message = "Unable to get list of files in diectory " + directory + ". Error is: " + error;
                logMessage(message, LOG_TYPE.ERROR, true);
                throw new CannotEditException(message);
            } else {
                logMessage("Successfully got list of files in directory " + directory, LOG_TYPE.INFO, true);
                return files;
            }
        } catch (CannotEditException | IOException e) {
            logMessage("Could not get list of files in directory " + directory, LOG_TYPE.ERROR, true);
            logMessage(e.getMessage(), LOG_TYPE.ERROR, true);
            throw e;
        }
    }

    /**
     * Delete a single file.
     *
     * @param full_file_name the path + name of the file
     * @throws IOException an IO error occurred when trying to delete the file
     * @throws CannotEditException unable to delete the file
     */
    public void fileDelete(String full_file_name) throws IOException, CannotEditException {
        try {
            logMessage("Attempting to delete file " + full_file_name, LOG_TYPE.INFO, true);
            String command = "del \"" + full_file_name + "\"";
            ProcessBuilder builder = new ProcessBuilder("C:\\Windows\\System32\\cmd.exe", "/c", command);
            builder.redirectErrorStream(true);
            Process cmd_process = builder.start();
            BufferedReader cmd_process_output_stream = new BufferedReader(new InputStreamReader(cmd_process.getInputStream()));
            String line = "";
            String error = "";
            while ((line = cmd_process_output_stream.readLine()) != null) {
                error = line;
            }
            if (error.compareTo("") != 0) {
                String message = "Unable to delete file " + full_file_name + ". Error is: " + error;
                logMessage(message, LOG_TYPE.ERROR, true);
                throw new CannotEditException(message);
            }
            logMessage("Successfully deleted file " + full_file_name, LOG_TYPE.INFO, true);
        } catch (CannotEditException | IOException e) {
            logMessage("Could not delete file " + full_file_name, LOG_TYPE.ERROR, true);
            logMessage(e.getMessage(), LOG_TYPE.ERROR, true);
            throw e;
        }
    }

    /**
     * Copies a single file
     *
     * @param old_full_file_name the path + name of the file to copy. Can be
     * copied from a remote computer using \\computername
     * @param new_directory the folder to copy the file to
     * @throws IOException an IO error occurred when copying the file
     * @throws CannotEditException unable to copy the file
     */
    public void fileCopy(String old_full_file_name, String new_directory) throws IOException, CannotEditException {
        try {
            logMessage("Attempting to copy file " + old_full_file_name + " to new directory " + new_directory, LOG_TYPE.INFO, true);
            String command = "copy \"" + old_full_file_name + "\" \"" + new_directory + "\"";
            ProcessBuilder builder = new ProcessBuilder("C:\\Windows\\System32\\cmd.exe", "/c", command);
            builder.redirectErrorStream(true);
            Process cmd_process = builder.start();
            BufferedReader cmd_process_output_stream = new BufferedReader(new InputStreamReader(cmd_process.getInputStream()));
            String line = "";
            String error = "";
            while ((line = cmd_process_output_stream.readLine()) != null) {
                error = line;
            }
            if (!error.contains("file(s) copied")) {
                String message = "Unable to copy file " + old_full_file_name + " to folder " + new_directory + ". Error is: " + error;
                logMessage(message, LOG_TYPE.ERROR, true);
                throw new CannotEditException(message);
            }
            logMessage("Successfully copied file " + old_full_file_name + " to new directory " + new_directory, LOG_TYPE.INFO, true);
        } catch (CannotEditException | IOException e) {
            logMessage("Could not copy file " + old_full_file_name + " to new directory " + new_directory, LOG_TYPE.ERROR, true);
            logMessage(e.getMessage(), LOG_TYPE.ERROR, true);
            throw e;
        }
    }

    /**
     * Creates a registry backup using pstools.
     *
     * @param computer the computer to create the backup on
     * @param reg_key the registry key to backup
     * @param full_file_name the path + filename of the backup file to create.
     * Include the file extension
     * @throws IOException an IO error occurred when backing up the registry or
     * writing the file
     * @throws CannotEditException unable to access the registry key or unable
     * to create the backup file
     * @throws InterruptedException the pstools process thread was interrupted
     */
    public void registryBackup(String computer, String reg_key, String full_file_name) throws IOException, CannotEditException, InterruptedException {
        try {
            logMessage("Attempting to backup registry key " + reg_key + " on computer " + computer + " to folder " + full_file_name, LOG_TYPE.INFO, true);
            String command = ".\\pstools\\psexec \\\\" + computer + " REG EXPORT \"" + reg_key + "\" \"" + full_file_name + "\" /y";
            ProcessBuilder builder = new ProcessBuilder("C:\\Windows\\System32\\cmd.exe", "/c", command);
            builder.redirectErrorStream(true);
            Process cmd_process = builder.start();
            BufferedReader cmd_process_output_stream = new BufferedReader(new InputStreamReader(cmd_process.getInputStream()));
            String line = "";
            String error = "";
            boolean run = true;
            while ((line = cmd_process_output_stream.readLine()) != null && run) {
                error = line;
                if (error.contains("REG exited")) {
                    run = false;
                }
            }
            cmd_process.waitFor();
            if (!error.contains("with error code 0")) {
                String message = "Could not backup registry key " + reg_key + " on computer " + computer + " to folder " + full_file_name;
                logMessage(message, LOG_TYPE.ERROR, true);
                throw new CannotEditException(message);
            }
            logMessage("Successfully backed up registry key " + reg_key + " on computer " + computer + " to folder " + full_file_name, LOG_TYPE.INFO, true);
        } catch (IOException | InterruptedException e) {
            logMessage("Could not backup registry key " + reg_key + " on computer " + computer + " to folder " + full_file_name + ". Error is " + e.getMessage(), LOG_TYPE.ERROR, true);
            throw e;
        }
    }

    /**
     * Deletes a registry key using pstools.
     *
     * @param computer the computer to delete the registry key from
     * @param reg_key the registry key to delete
     * @throws IOException an IO error occurred when trying to delete the
     * registry key
     * @throws InterruptedException the pstools process thread was interrupted
     */
    public void registryDelete(String computer, String reg_key) throws IOException, InterruptedException {
        try {
            logMessage("Attempting to delete registry key " + reg_key + " from computer " + computer, LOG_TYPE.INFO, true);
            String command = ".\\pstools\\psexec \\\\" + computer + " REG DELETE \"" + reg_key + "\" /f";
            ProcessBuilder builder = new ProcessBuilder("C:\\Windows\\System32\\cmd.exe", "/c", command);
            builder.redirectErrorStream(true);
            Process cmd_process = builder.start();
            cmd_process.waitFor();
            logMessage("Successfully deleted registry key " + reg_key + " from computer " + computer, LOG_TYPE.INFO, true);
        } catch (IOException | InterruptedException e) {
            logMessage("Could not delete registry key " + reg_key + " from computer " + computer, LOG_TYPE.ERROR, true);
            logMessage(e.getMessage(), LOG_TYPE.ERROR, true);
            throw e;
        }
    }

    /**
     * Compiles the user list into a single readable String.
     * <p>
     * The String includes \n and \t characters to aid formatting.
     *
     * @return readable String containing the user list data
     */
    public String printUserList() {
        logMessage("Compiling user list into readable String", LOG_TYPE.INFO, true, false);
        String output = "";
        Double total_size = 0.0;
        output += UserData.headingsToString();
        if (user_list.size() > 0) {
            output += '\n';
        }
        for (int i = 0; i < user_list.size(); i++) {
            output += user_list.get(i).toString();
            if (Pattern.matches("[-+]?[0-9]*\\.?[0-9]+", user_list.get(i).getSize())) {
                total_size += Double.parseDouble(user_list.get(i).getSize());
            }
            if (i != user_list.size() - 1) {
                output += '\n';
            }
        }
        if (user_list.size() > 0) {
            Double size_in_megabytes = total_size / (1024.0 * 1024.0);
            output += '\n' + "Total size:" + '\t' + (size_in_megabytes + " MB");
        }
        logMessage("Successfully compiled user list into readable String", LOG_TYPE.INFO, true, false);
        return output;
    }

    /**
     * Generates a String using the current date/time.
     *
     * @return the generated String based on the current date/time
     */
    public String generateDateString() {
        String output = generateDateString("");
        return output;
    }

    /**
     * Generates a String using the current date/time.
     * <p>
     * Can supply a prefix to add to the front of the generated String.
     *
     * @param prefix the prefix to add to the front of the generated String.
     * @return the generated String based on the current date/time and prefix
     * supplied
     */
    public String generateDateString(String prefix) {
        logMessage("Generating date/time String with prefix " + prefix, LOG_TYPE.INFO, true, false);
        TimeZone timezone = TimeZone.getTimeZone("UTC");
        Calendar calendar = Calendar.getInstance();
        SimpleDateFormat filename_utc = new SimpleDateFormat("yyMMddHHmmss");
        String current_date = filename_utc.format(calendar.getTime());
        logMessage("Generated date/time String " + prefix + current_date, LOG_TYPE.INFO, true, false);
        return prefix + current_date;
    }

    /**
     * Adds a message to the log.
     * <p>
     * Requires a severity using the LOG_TYPE enum and can choose to include a
     * timestamp.
     *
     * @param message the message to add to the log
     * @param severity the severity LOG_TYPE of the message
     * @param include_timestamp whether to include a timestamp or not
     */
    public void logMessage(String message, LOG_TYPE severity, boolean include_timestamp) {
        logMessage(message, severity, include_timestamp, true);
    }

    /**
     * Adds a message to the log.
     * <p>
     * Requires a severity using the LOG_TYPE enum and can choose to include a
     * timestamp.<br>
     * If an ActionListener has been specified on the ProfileDeleter class it
     * will trigger a "LogWritten" ActionEvent on the ActionListener. This is
     * intended to allow any GUI classes to update any elements used to display
     * the log as it is updated.
     *
     * @param message the message to add to the log
     * @param severity the severity LOG_TYPE of the message
     * @param include_timestamp whether to include a timestamp or not
     * @param display_to_gui triggers a "LogWritten" action event if an
     * ActionListener has been specified on the ProfileDeleter class
     */
    public void logMessage(String message, LOG_TYPE severity, boolean include_timestamp, boolean display_to_gui) {
        String log_message = "";
        if (null != severity) {
            switch (severity) {
                case INFO:
                    log_message += "Info: ";
                    break;
                case WARNING:
                    log_message += "Warning: ";
                    break;
                case ERROR:
                    log_message += "ERROR: ";
                    break;
                default:
                    break;
            }
        }

        if (include_timestamp) {
            SimpleDateFormat human_readable_timestamp = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss.SSS");
            Date timestamp = new Date();
            String format_timestamp = human_readable_timestamp.format(timestamp);
            log_message += "[" + format_timestamp + "] ";
        }

        log_message += message;

        log_list.add(log_message);
        if (display_to_gui && log_updated != null) {
            log_updated.actionPerformed(new java.awt.event.ActionEvent(this, 0, "LogWritten"));
        }
    }

    /**
     * Dumps the list of logged messages to a text file.
     *
     * @return the filename of the created text file
     * @throws IOException an IO error occurred when trying to create the text
     * file
     * @throws NotInitialisedException the log list attribute has not had
     * anything logged to it
     */
    public String writeLog() throws IOException, NotInitialisedException {
        if (!log_list.isEmpty()) {
            try {
                String filename = "logs\\Profile_Deleter_Log_" + generateDateString() + ".txt";
                writeToFile(filename, log_list);
                return filename;
            } catch (IOException e) {
                throw e;
            }
        } else {
            throw new NotInitialisedException("Nothing has been logged");
        }
    }

    /**
     * Reads all lines in a file and adds the to a String list.
     *
     * @param filename the path + filename of the file to read
     * @return the contents of the file compiled into a String list
     * @throws IOException an IO error occurred when trying to read the file
     */
    public List<String> readFromFile(String filename) throws IOException {
        List<String> read_data = new ArrayList<String>();
        try {
            File file = new File(filename);
            try (BufferedReader br = new BufferedReader(new FileReader(file))) {
                for (String line; (line = br.readLine()) != null;) {
                    read_data.add(line);
                }
            }
        } catch (IOException e) {
            throw e;
        }
        return read_data;
    }

    /**
     * Write all lines in a String list to a file.
     *
     * @param filename the path + filename of the file to write to
     * @param write_to_file the String list to write to the file
     * @throws IOException an IO error occurred when trying to write to the file
     */
    public void writeToFile(String filename, List<String> write_to_file) throws IOException {
        try {
            int count = 0;
            File file = new File(filename);
            BufferedWriter writer = new BufferedWriter(new FileWriter(file, true));
            for (String string_line : write_to_file) {
                if (count > 0) {
                    writer.newLine();
                }
                writer.write(string_line);
                count++;
            }
            writer.close();
        } catch (IOException e) {
            throw e;
        }
    }

    /**
     * Pings a computer to see if it is reachable on the network.
     * <p>
     * Can be supplied a hostname or IP address.
     *
     * @param PC the hostname or IP address of the computer to ping
     * @return whether the computer is reachable on the network or not
     * @throws IOException an IO error occurred when trying to run the process
     * to ping the computer
     * @throws InterruptedException the cmd process thread was interrupted
     */
    public boolean pingPC(String PC) throws IOException, InterruptedException {
        logMessage("Pinging PC " + PC + " to ensure it exists and is reachable on the network", LOG_TYPE.INFO, true);
        boolean pc_online = false;
        try {
            String command = "ping " + PC + " -n 1";
            ProcessBuilder builder = new ProcessBuilder("C:\\Windows\\System32\\cmd.exe", "/c", command);
            builder.redirectErrorStream(true);
            Process p = builder.start();
            BufferedReader r = new BufferedReader(new InputStreamReader(p.getInputStream()));
            p.waitFor();

            boolean search = true;
            String detail = "";
            while (search) {
                detail = r.readLine();
                if (detail == null) {
                    pc_online = false;
                    search = false;
                } else if (detail.contains("Received = 1")) {
                    pc_online = true;
                    search = false;
                }
            }
        } catch (IOException | InterruptedException e) {
            logMessage("Ping check has failed with error " + e.getMessage(), LOG_TYPE.ERROR, true);
            throw e;
        }
        logMessage("Ping check has completed, result is " + pc_online, LOG_TYPE.INFO, true);
        return pc_online;
    }
}
