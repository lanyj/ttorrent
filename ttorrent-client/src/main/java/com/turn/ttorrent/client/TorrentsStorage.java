package com.turn.ttorrent.client;

import com.turn.ttorrent.common.AnnounceableFileTorrent;
import com.turn.ttorrent.common.AnnounceableTorrent;
import com.turn.ttorrent.common.Pair;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

public class TorrentsStorage {

  private final ReadWriteLock myReadWriteLock;
  private final Map<String, SharedTorrent> myActiveTorrents;
  private final Map<String, AnnounceableFileTorrent> myAnnounceableTorrents;

  public TorrentsStorage() {
    myReadWriteLock = new ReentrantReadWriteLock();
    myActiveTorrents = new HashMap<String, SharedTorrent>();
    myAnnounceableTorrents = new HashMap<String, AnnounceableFileTorrent>();
  }

  public boolean hasTorrent(String hash) {
    try {
      myReadWriteLock.readLock().lock();
      return myAnnounceableTorrents.containsKey(hash);
    } finally {
      myReadWriteLock.readLock().unlock();
    }
  }

  public AnnounceableFileTorrent getAnnounceableTorrent(String hash) {
    try {
      myReadWriteLock.readLock().lock();
      return myAnnounceableTorrents.get(hash);
    } finally {
      myReadWriteLock.readLock().unlock();
    }
  }

  public void peerDisconnected(String torrentHash) {
    final SharedTorrent torrent;
    try {
      myReadWriteLock.writeLock().lock();
      torrent = myActiveTorrents.get(torrentHash);
      if (torrent == null) return;

      if (torrent.getDownloadersCount() == 0) {
        myActiveTorrents.remove(torrentHash);
      }
    } finally {
      myReadWriteLock.writeLock().unlock();
    }
    torrent.close();
  }

  public SharedTorrent getTorrent(String hash) {
    try {
      myReadWriteLock.readLock().lock();
      return myActiveTorrents.get(hash);
    } finally {
      myReadWriteLock.readLock().unlock();
    }
  }

  public void addAnnounceableTorrent(String hash, AnnounceableFileTorrent torrent) {
    try {
      myReadWriteLock.writeLock().lock();
      myAnnounceableTorrents.put(hash, torrent);
    } finally {
      myReadWriteLock.writeLock().unlock();
    }
  }

  public SharedTorrent putIfAbsentActiveTorrent(String hash, SharedTorrent torrent) {
    try {
      myReadWriteLock.writeLock().lock();
      final SharedTorrent old = myActiveTorrents.get(hash);
      if (old != null) return old;

      return myActiveTorrents.put(hash, torrent);
    } finally {
      myReadWriteLock.writeLock().unlock();
    }
  }

  public Pair<SharedTorrent, AnnounceableFileTorrent> remove(String hash) {
    try {
      myReadWriteLock.writeLock().lock();
      final SharedTorrent sharedTorrent = myActiveTorrents.remove(hash);
      final AnnounceableFileTorrent announceableFileTorrent = myAnnounceableTorrents.remove(hash);
      return new Pair<SharedTorrent, AnnounceableFileTorrent>(sharedTorrent, announceableFileTorrent);
    } finally {
      myReadWriteLock.writeLock().unlock();
    }
  }

  public List<SharedTorrent> activeTorrents() {
    try {
      myReadWriteLock.readLock().lock();
      return new ArrayList<SharedTorrent>(myActiveTorrents.values());
    } finally {
      myReadWriteLock.readLock().unlock();
    }
  }

  public List<AnnounceableTorrent> announceableTorrents() {
    try {
      myReadWriteLock.readLock().lock();
      return new ArrayList<AnnounceableTorrent>(myAnnounceableTorrents.values());
    } finally {
      myReadWriteLock.readLock().unlock();
    }
  }

  public void clear() {
    try {
      myReadWriteLock.writeLock().lock();
      myAnnounceableTorrents.clear();
      myActiveTorrents.clear();
    } finally {
      myReadWriteLock.writeLock().unlock();
    }
  }
}