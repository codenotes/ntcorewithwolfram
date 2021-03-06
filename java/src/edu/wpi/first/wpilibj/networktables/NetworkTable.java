package edu.wpi.first.wpilibj.networktables;

import edu.wpi.first.wpilibj.tables.*;
import edu.wpi.first.wpilibj.networktables2.type.*;
import java.io.*;
import java.nio.ByteBuffer;
import java.util.*;

/**
 * A network table that knows its subtable path.
 */
public class NetworkTable implements ITable, IRemote {
  /**
   * The path separator for sub-tables and keys
   *
   */
  public static final char PATH_SEPARATOR = '/';
  /**
   * The default port that network tables operates on
   */
  public static final int DEFAULT_PORT = 1735;

  private static boolean client = false;
  private static boolean running = false;
  private static int port = DEFAULT_PORT;
  private static String ipAddress = "";
  private static String persistentFilename = "networktables.ini";

  private synchronized static void checkInit() {
    if (running)
      throw new IllegalStateException(
          "Network tables has already been initialized");
  }

  /**
   * initializes network tables
   */
  public synchronized static void initialize() {
    if (running)
      shutdown();
    if (client)
      NetworkTablesJNI.startClient(ipAddress, port);
    else
      NetworkTablesJNI.startServer(persistentFilename, "", port);
    running = true;
  }

  /**
   * shuts down network tables
   */
  public synchronized static void shutdown() {
    if (!running)
      return;
    if (client)
      NetworkTablesJNI.stopClient();
    else
      NetworkTablesJNI.stopServer();
    running = false;
  }

  /**
   * set that network tables should be a server
   * This must be called before initialize or getTable
   */
  public synchronized static void setServerMode() {
    if (!client)
      return;
    checkInit();
    client = false;
  }

  /**
   * set that network tables should be a client
   * This must be called before initialize or getTable
   */
  public synchronized static void setClientMode() {
    if (client)
      return;
    checkInit();
    client = true;
  }

  /**
   * set the team the robot is configured for (this will set the mdns address that
   * network tables will connect to in client mode)
   * This must be called before initialize or getTable
   * @param team the team number
   */
  public synchronized static void setTeam(int team) {
    setIPAddress("roboRIO-" + team + "-FRC.local");
  }

  /**
   * @param address the adress that network tables will connect to in client
   * mode
   */
  public synchronized static void setIPAddress(final String address) {
    if (ipAddress.equals(address))
      return;
    checkInit();
    ipAddress = address;
  }

  /**
   * @param aport the port number that network tables will connect to in client
   * mode or listen to in server mode
   */
  public synchronized static void setPort(int aport) {
    if (port == aport)
      return;
    checkInit();
    port = aport;
  }

  /**
   * Sets the persistent filename.
   * @param filename the filename that the network tables server uses for
   * automatic loading and saving of persistent values
   */
  public synchronized static void setPersistentFilename(final String filename) {
    if (persistentFilename.equals(filename))
      return;
    checkInit();
    persistentFilename = filename;
  }

  /**
   * Sets the network identity.
   * This is provided in the connection info on the remote end.
   * @param name identity
   */
  public static void setNetworkIdentity(String name) {
    NetworkTablesJNI.setNetworkIdentity(name);
  }

  public static boolean[] toNative(Boolean[] arr) {
    boolean[] out = new boolean[arr.length];
    for (int i = 0; i < arr.length; i++)
      out[i] = arr[i];
    return out;
  }

  public static double[] toNative(Double[] arr) {
    double[] out = new double[arr.length];
    for (int i = 0; i < arr.length; i++)
      out[i] = arr[i];
    return out;
  }

  public static Boolean[] fromNative(boolean[] arr) {
    Boolean[] out = new Boolean[arr.length];
    for (int i = 0; i < arr.length; i++)
      out[i] = arr[i];
    return out;
  }

  public static Double[] fromNative(double[] arr) {
    Double[] out = new Double[arr.length];
    for (int i = 0; i < arr.length; i++)
      out[i] = arr[i];
    return out;
  }

  /**
   * Gets the table with the specified key. If the table does not exist, a new
   *table will be created.<br>
   * This will automatically initialize network tables if it has not been
   *already
   *
   * @param key
   *            the key name
   * @return the network table requested
   */
  public synchronized static NetworkTable getTable(String key) {
    if (!running)
      initialize();
    if (key.isEmpty() || key.charAt(0) == PATH_SEPARATOR)
      return new NetworkTable(key);
    return new NetworkTable(PATH_SEPARATOR + key);
  }

  private final String path;

  NetworkTable(String path) {
    this.path = path;
  }
  public String toString() { return "NetworkTable: " + path; }

  public static ConnectionInfo[] connections() {
    return NetworkTablesJNI.getConnections();
  }

  public boolean isConnected() {
    ConnectionInfo[] conns = NetworkTablesJNI.getConnections();
    return conns.length > 0;
  }

  public boolean isServer() {
    return !client;
  }

  private class ListenerBase {
    public int uid;
  }

  private class ConnectionListenerAdapter extends ListenerBase implements NetworkTablesJNI.ConnectionListenerFunction {
    private final IRemote targetSource;
    private final IRemoteConnectionListener targetListener;

    public ConnectionListenerAdapter(IRemote targetSource, IRemoteConnectionListener targetListener) {
      this.targetSource = targetSource;
      this.targetListener = targetListener;
    }

    public void apply(int uid, boolean connected, ConnectionInfo conn) {
      if (connected)
        targetListener.connected(targetSource);
      else
        targetListener.disconnected(targetSource);
    }
  }

  private final Hashtable<IRemoteConnectionListener,ConnectionListenerAdapter> connectionListenerMap = new Hashtable<IRemoteConnectionListener,ConnectionListenerAdapter>();
  public synchronized void addConnectionListener(IRemoteConnectionListener listener,
                                                 boolean immediateNotify) {
    ConnectionListenerAdapter adapter = connectionListenerMap.get(listener);
    if (adapter != null)
      throw new IllegalStateException("Cannot add the same listener twice");
    adapter = new ConnectionListenerAdapter(this, listener);
    adapter.uid = NetworkTablesJNI.addConnectionListener(adapter, immediateNotify);
    connectionListenerMap.put(listener, adapter);
  }

  public synchronized void removeConnectionListener(IRemoteConnectionListener listener) {
    ConnectionListenerAdapter adapter = connectionListenerMap.get(listener);
    if (adapter != null) {
      NetworkTablesJNI.removeConnectionListener(adapter.uid);
      connectionListenerMap.remove(listener);
    }
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void addTableListener(ITableListener listener) {
    addTableListenerEx(listener, NOTIFY_NEW | NOTIFY_UPDATE);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void addTableListener(ITableListener listener,
                               boolean immediateNotify) {
    int flags = NOTIFY_NEW | NOTIFY_UPDATE;
    if (immediateNotify)
      flags |= NOTIFY_IMMEDIATE;
    addTableListenerEx(listener, flags);
  }

  private class TableListenerAdapter extends ListenerBase implements NetworkTablesJNI.EntryListenerFunction {
    private final int prefixLen;
    private final ITable targetSource;
    private final ITableListener targetListener;

    public TableListenerAdapter(int prefixLen, ITable targetSource, ITableListener targetListener) {
      this.prefixLen = prefixLen;
      this.targetSource = targetSource;
      this.targetListener = targetListener;
    }

    public void apply(int uid, String key, Object value, int flags) {
      String relativeKey = key.substring(prefixLen);
      if (relativeKey.indexOf(PATH_SEPARATOR) != -1)
        return;
      targetListener.valueChangedEx(targetSource, relativeKey, value, flags);
    }
  }

  private final Hashtable<ITableListener,List<ListenerBase>> listenerMap = new Hashtable<ITableListener,List<ListenerBase>>();
  public synchronized void addTableListenerEx(ITableListener listener,
                                              int flags) {
    List<ListenerBase> adapters = listenerMap.get(listener);
    if (adapters == null) {
      adapters = new ArrayList<ListenerBase>();
      listenerMap.put(listener, adapters);
    }
    TableListenerAdapter adapter =
        new TableListenerAdapter(path.length() + 1, this, listener);
    adapter.uid = NetworkTablesJNI.addEntryListener(path + PATH_SEPARATOR, adapter, flags);
    adapters.add(adapter);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void addTableListener(String key, ITableListener listener,
                               boolean immediateNotify) {
    int flags = NOTIFY_NEW | NOTIFY_UPDATE;
    if (immediateNotify)
      flags |= NOTIFY_IMMEDIATE;
    addTableListenerEx(key, listener, flags);
  }

  private class KeyListenerAdapter extends ListenerBase implements NetworkTablesJNI.EntryListenerFunction {
    private final String relativeKey;
    private final String fullKey;
    private final ITable targetSource;
    private final ITableListener targetListener;

    public KeyListenerAdapter(String relativeKey, String fullKey, ITable targetSource, ITableListener targetListener) {
      this.relativeKey = relativeKey;
      this.fullKey = fullKey;
      this.targetSource = targetSource;
      this.targetListener = targetListener;
    }

    public void apply(int uid, String key, Object value, int flags) {
      if (!key.equals(fullKey))
        return;
      targetListener.valueChangedEx(targetSource, relativeKey, value, flags);
    }
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public synchronized void addTableListenerEx(String key,
                                              ITableListener listener,
                                              int flags) {
    List<ListenerBase> adapters = listenerMap.get(listener);
    if (adapters == null) {
      adapters = new ArrayList<ListenerBase>();
      listenerMap.put(listener, adapters);
    }
    String fullKey = path + PATH_SEPARATOR + key;
    KeyListenerAdapter adapter =
        new KeyListenerAdapter(key, fullKey, this, listener);
    adapter.uid = NetworkTablesJNI.addEntryListener(fullKey, adapter, flags);
    adapters.add(adapter);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void addSubTableListener(final ITableListener listener) {
    addSubTableListener(listener, false);
  }

  private class SubListenerAdapter extends ListenerBase implements NetworkTablesJNI.EntryListenerFunction {
    private final int prefixLen;
    private final ITable targetSource;
    private final ITableListener targetListener;
    private final Set<String> notifiedTables = new HashSet<String>();

    public SubListenerAdapter(int prefixLen, ITable targetSource, ITableListener targetListener) {
      this.prefixLen = prefixLen;
      this.targetSource = targetSource;
      this.targetListener = targetListener;
    }

    public void apply(int uid, String key, Object value, int flags) {
      String relativeKey = key.substring(prefixLen);
      int endSubTable = relativeKey.indexOf(PATH_SEPARATOR);
      if (endSubTable == -1)
        return;
      String subTableKey = relativeKey.substring(0, endSubTable);
      if (notifiedTables.contains(subTableKey))
        return;
      notifiedTables.add(subTableKey);
      targetListener.valueChangedEx(targetSource, subTableKey, targetSource.getSubTable(subTableKey), flags);
    }
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public synchronized void addSubTableListener(final ITableListener listener,
                                               boolean localNotify) {
    List<ListenerBase> adapters = listenerMap.get(listener);
    if (adapters == null) {
      adapters = new ArrayList<ListenerBase>();
      listenerMap.put(listener, adapters);
    }
    SubListenerAdapter adapter =
        new SubListenerAdapter(path.length() + 1, this, listener);
    int flags = NOTIFY_NEW | NOTIFY_IMMEDIATE;
    if (localNotify)
      flags |= NOTIFY_LOCAL;
    adapter.uid = NetworkTablesJNI.addEntryListener(path + PATH_SEPARATOR, adapter, flags);
    adapters.add(adapter);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public synchronized void removeTableListener(ITableListener listener) {
    List<ListenerBase> adapters = listenerMap.get(listener);
    if (adapters != null) {
      for (int i = 0; i < adapters.size(); ++i)
        NetworkTablesJNI.removeEntryListener(adapters.get(i).uid);
      adapters.clear();
    }
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public ITable getSubTable(String key) {
    return new NetworkTable(path + PATH_SEPARATOR + key);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public boolean containsKey(String key) {
    return NetworkTablesJNI.containsKey(path + PATH_SEPARATOR + key);
  }

  public boolean containsSubTable(String key) {
    EntryInfo[] entries = NetworkTablesJNI.getEntries(path + PATH_SEPARATOR + key + PATH_SEPARATOR, 0);
    return entries.length != 0;
  }

  /**
   * @param types bitmask of types; 0 is treated as a "don't care".
   * @return keys currently in the table
   */
  public Set<String> getKeys(int types) {
    Set<String> keys = new HashSet<String>();
    int prefixLen = path.length() + 1;
    for (EntryInfo entry : NetworkTablesJNI.getEntries(path + PATH_SEPARATOR, types)) {
      String relativeKey = entry.name.substring(prefixLen);
      if (relativeKey.indexOf(PATH_SEPARATOR) != -1)
        continue;
      keys.add(relativeKey);
    }
    return keys;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Set<String> getKeys() {
    return getKeys(0);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Set<String> getSubTables() {
    Set<String> keys = new HashSet<String>();
    int prefixLen = path.length() + 1;
    for (EntryInfo entry : NetworkTablesJNI.getEntries(path + PATH_SEPARATOR, 0)) {
      String relativeKey = entry.name.substring(prefixLen);
      int endSubTable = relativeKey.indexOf(PATH_SEPARATOR);
      if (endSubTable == -1)
        continue;
      keys.add(relativeKey.substring(0, endSubTable));
    }
    return keys;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public boolean putNumber(String key, double value) {
    return NetworkTablesJNI.putDouble(path + PATH_SEPARATOR + key, value);
  }

  /**
   * {@inheritDoc}
   * @deprecated This exception-raising method has been replaced by the
   * default-taking method {@link #getNumber(String, double)}.
   */
  @Override
  @Deprecated
  public double getNumber(String key) throws TableKeyNotDefinedException {
    return NetworkTablesJNI.getDouble(path + PATH_SEPARATOR + key);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public double getNumber(String key, double defaultValue) {
    return NetworkTablesJNI.getDouble(path + PATH_SEPARATOR + key, defaultValue);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public boolean putString(String key, String value) {
    return NetworkTablesJNI.putString(path + PATH_SEPARATOR + key, value);
  }

  /**
   * {@inheritDoc}
   * @deprecated This exception-raising method has been replaced by the
   * default-taking method {@link #getString(String, String)}.
   */
  @Override
  @Deprecated
  public String getString(String key) throws TableKeyNotDefinedException {
    return NetworkTablesJNI.getString(path + PATH_SEPARATOR + key);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public String getString(String key, String defaultValue) {
    return NetworkTablesJNI.getString(path + PATH_SEPARATOR + key, defaultValue);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public boolean putBoolean(String key, boolean value) {
    return NetworkTablesJNI.putBoolean(path + PATH_SEPARATOR + key, value);
  }

  /**
   * {@inheritDoc}
   * @deprecated This exception-raising method has been replaced by the
   * default-taking method {@link #getBoolean(String, boolean)}.
   */
  @Override
  @Deprecated
  public boolean getBoolean(String key) throws TableKeyNotDefinedException {
    return NetworkTablesJNI.getBoolean(path + PATH_SEPARATOR + key);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public boolean getBoolean(String key, boolean defaultValue) {
    return NetworkTablesJNI.getBoolean(path + PATH_SEPARATOR + key, defaultValue);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public boolean putBooleanArray(String key, boolean[] value) {
    return NetworkTablesJNI.putBooleanArray(path + PATH_SEPARATOR + key, value);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public boolean putBooleanArray(String key, Boolean[] value) {
    return putBooleanArray(key, toNative(value));
  }

  /**
   * {@inheritDoc}
   * @deprecated This exception-raising method has been replaced by the
   * default-taking method {@link #getBooleanArray(String, boolean[])}.
   */
  @Override
  @Deprecated
  public boolean[] getBooleanArray(String key) throws TableKeyNotDefinedException {
    return NetworkTablesJNI.getBooleanArray(path + PATH_SEPARATOR + key);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public boolean[] getBooleanArray(String key, boolean[] defaultValue) {
    return NetworkTablesJNI.getBooleanArray(path + PATH_SEPARATOR + key, defaultValue);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Boolean[] getBooleanArray(String key, Boolean[] defaultValue) {
    try {
      return fromNative(getBooleanArray(key));
    } catch (TableKeyNotDefinedException e) {
      return defaultValue;
    }
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public boolean putNumberArray(String key, double[] value) {
    return NetworkTablesJNI.putDoubleArray(path + PATH_SEPARATOR + key, value);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public boolean putNumberArray(String key, Double[] value) {
    return putNumberArray(key, toNative(value));
  }

  /**
   * {@inheritDoc}
   * @deprecated This exception-raising method has been replaced by the
   * default-taking method {@link #getNumberArray(String, double[])}.
   */
  @Override
  @Deprecated
  public double[] getNumberArray(String key) throws TableKeyNotDefinedException {
    return NetworkTablesJNI.getDoubleArray(path + PATH_SEPARATOR + key);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public double[] getNumberArray(String key, double[] defaultValue) {
    return NetworkTablesJNI.getDoubleArray(path + PATH_SEPARATOR + key, defaultValue);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Double[] getNumberArray(String key, Double[] defaultValue) {
    try {
      return fromNative(getNumberArray(key));
    } catch (TableKeyNotDefinedException e) {
      return defaultValue;
    }
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public boolean putStringArray(String key, String[] value) {
    return NetworkTablesJNI.putStringArray(path + PATH_SEPARATOR + key, value);
  }

  /**
   * {@inheritDoc}
   * @deprecated This exception-raising method has been replaced by the
   * default-taking method {@link #getStringArray(String, String[])}.
   */
  @Override
  @Deprecated
  public String[] getStringArray(String key) throws TableKeyNotDefinedException {
    return NetworkTablesJNI.getStringArray(path + PATH_SEPARATOR + key);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public String[] getStringArray(String key, String[] defaultValue) {
    return NetworkTablesJNI.getStringArray(path + PATH_SEPARATOR + key, defaultValue);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public boolean putRaw(String key, byte[] value) {
    return NetworkTablesJNI.putRaw(path + PATH_SEPARATOR + key, value);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public boolean putRaw(String key, ByteBuffer value, int len) {
    if (!value.isDirect())
      throw new IllegalArgumentException("must be a direct buffer");
    if (value.capacity() < len)
      throw new IllegalArgumentException("buffer is too small, must be at least " + len);
    return NetworkTablesJNI.putRaw(path + PATH_SEPARATOR + key, value, len);
  }

  /**
   * {@inheritDoc}
   * @deprecated This exception-raising method has been replaced by the
   * default-taking method {@link #getRaw(String, byte[])}.
   */
  @Override
  @Deprecated
  public byte[] getRaw(String key) throws TableKeyNotDefinedException {
    return NetworkTablesJNI.getRaw(path + PATH_SEPARATOR + key);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public byte[] getRaw(String key, byte[] defaultValue) {
    return NetworkTablesJNI.getRaw(path + PATH_SEPARATOR + key, defaultValue);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public boolean putValue(String key, Object value) throws IllegalArgumentException {
    if (value instanceof Boolean)
      return NetworkTablesJNI.putBoolean(path + PATH_SEPARATOR + key, ((Boolean)value).booleanValue());
    else if (value instanceof Double)
      return NetworkTablesJNI.putDouble(path + PATH_SEPARATOR + key, ((Double)value).doubleValue());
    else if (value instanceof String)
      return NetworkTablesJNI.putString(path + PATH_SEPARATOR + key, (String)value);
    else if (value instanceof byte[])
      return NetworkTablesJNI.putRaw(path + PATH_SEPARATOR + key, (byte[])value);
    else if (value instanceof boolean[])
      return NetworkTablesJNI.putBooleanArray(path + PATH_SEPARATOR + key, (boolean[])value);
    else if (value instanceof double[])
      return NetworkTablesJNI.putDoubleArray(path + PATH_SEPARATOR + key, (double[])value);
    else if (value instanceof Boolean[])
      return NetworkTablesJNI.putBooleanArray(path + PATH_SEPARATOR + key, toNative((Boolean[])value));
    else if (value instanceof Double[])
      return NetworkTablesJNI.putDoubleArray(path + PATH_SEPARATOR + key, toNative((Double[])value));
    else if (value instanceof String[])
      return NetworkTablesJNI.putStringArray(path + PATH_SEPARATOR + key, (String[])value);
    else if (value instanceof BooleanArray)
      return NetworkTablesJNI.putBooleanArray(path + PATH_SEPARATOR + key, toNative((Boolean[])((ArrayData)value).getDataArray()));
    else if (value instanceof NumberArray)
      return NetworkTablesJNI.putDoubleArray(path + PATH_SEPARATOR + key, toNative((Double[])((ArrayData)value).getDataArray()));
    else if (value instanceof StringArray)
      return NetworkTablesJNI.putStringArray(path + PATH_SEPARATOR + key, (String[])((ArrayData)value).getDataArray());
    else
      throw new IllegalArgumentException(key);
  }

  /**
   * {@inheritDoc}
   * @deprecated Use get*Array functions instead.
   */
  @Override
  @Deprecated
  public void retrieveValue(String key, Object externalData) throws TableKeyNotDefinedException {
    Object value = getValue(key);
    if (value instanceof boolean[] && externalData instanceof BooleanArray)
      ((ArrayData)externalData).setDataArray(fromNative((boolean[])value));
    else if (value instanceof double[] && externalData instanceof NumberArray)
      ((ArrayData)externalData).setDataArray(fromNative((double[])value));
    else if (value instanceof String[] && externalData instanceof StringArray)
      ((ArrayData)externalData).setDataArray((String[])value);
    else
      throw new TableKeyNotDefinedException(key);
  }

  /**
   * {@inheritDoc}
   * @deprecated This exception-raising method has been replaced by the
   * default-taking method {@link #getValue(String, Object)}.
   */
  @Override
  @Deprecated
  public Object getValue(String key) throws TableKeyNotDefinedException {
    return NetworkTablesJNI.getValue(path + PATH_SEPARATOR + key);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Object getValue(String key, Object defaultValue) {
    return NetworkTablesJNI.getValue(path + PATH_SEPARATOR + key, defaultValue);
  }

  /** The persistent flag value. */
  public static final int PERSISTENT = 1;

  /**
   * {@inheritDoc}
   */
  @Override
  public void setPersistent(String key) {
    setFlags(key, PERSISTENT);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void clearPersistent(String key) {
    clearFlags(key, PERSISTENT);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public boolean isPersistent(String key) {
    return (getFlags(key) & PERSISTENT) != 0;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void setFlags(String key, int flags) {
    NetworkTablesJNI.setEntryFlags(path + PATH_SEPARATOR + key, getFlags(key) | flags);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void clearFlags(String key, int flags) {
    NetworkTablesJNI.setEntryFlags(path + PATH_SEPARATOR + key, getFlags(key) & ~flags);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public int getFlags(String key) {
    return NetworkTablesJNI.getEntryFlags(path + PATH_SEPARATOR + key);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void delete(String key) {
    NetworkTablesJNI.deleteEntry(path + PATH_SEPARATOR + key);
  }

  /**
   * Deletes ALL keys in ALL subtables.  Use with caution!
   */
  public static void globalDeleteAll() {
    NetworkTablesJNI.deleteAllEntries();
  }

  /**
   * Flushes all updated values immediately to the network.
   * Note: This is rate-limited to protect the network from flooding.
   * This is primarily useful for synchronizing network updates with
   * user code.
   */
  public static void flush() {
    NetworkTablesJNI.flush();
  }

  /**
   * Set the periodic update rate.
   *
   * @param interval update interval in seconds (range 0.1 to 1.0)
   */
  public static void setUpdateRate(double interval) {
    NetworkTablesJNI.setUpdateRate(interval);
  }

  /**
   * Saves persistent keys to a file.  The server does this automatically.
   *
   * @param filename file name
   * @throws PersistentException if error saving file
   */
  public static void savePersistent(String filename) throws PersistentException {
    NetworkTablesJNI.savePersistent(filename);
  }

  /**
   * Loads persistent keys from a file.  The server does this automatically.
   *
   * @param filename file name
   * @return List of warnings (errors result in an exception instead)
   * @throws PersistentException if error reading file
   */
  public static String[] loadPersistent(String filename) throws PersistentException {
    return NetworkTablesJNI.loadPersistent(filename);
  }

  /*
   * Deprecated Methods
   */

  /**
   * {@inheritDoc}
   * @deprecated Use {@link #putNumber(String, double)} instead.
   */
  @Override
  @Deprecated
  public boolean putInt(String key, int value) {
    return putNumber(key, value);
  }

  /**
   * {@inheritDoc}
   * @deprecated Use {@link #getNumber(String, double)} instead.
   */
  @Override
  @Deprecated
  public int getInt(String key) throws TableKeyNotDefinedException {
    return (int)getNumber(key);
  }

  /**
   * {@inheritDoc}
   * @deprecated Use {@link #getNumber(String, double)} instead.
   */
  @Override
  @Deprecated
  public int getInt(String key, int defaultValue) throws TableKeyNotDefinedException {
    try {
      return (int)getNumber(key);
    } catch (NoSuchElementException ex) {
      return defaultValue;
    }
  }

  /**
   * {@inheritDoc}
   * @deprecated Use {@link #putNumber(String, double)} instead.
   */
  @Override
  @Deprecated
  public boolean putDouble(String key, double value) {
    return putNumber(key, value);
  }

  /**
   * {@inheritDoc}
   * @deprecated Use {@link #getNumber(String, double)} instead.
   */
  @Override
  @Deprecated
  public double getDouble(String key) throws TableKeyNotDefinedException {
    return getNumber(key);
  }

  /**
   * {@inheritDoc}
   * @deprecated Use {@link #getNumber(String, double)} instead.
   */
  @Override
  @Deprecated
  public double getDouble(String key, double defaultValue) {
    return getNumber(key, defaultValue);
  }
}
