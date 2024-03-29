package com.jamoes.lightsout;

import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Random;
import java.util.Set;

import android.graphics.Bitmap;
import android.os.Handler;
import android.os.SystemClock;

public class GameBoard {

  public static final int LAST_LEVEL = 6;
  
  private int size, level;
  private int totalSeconds, totalMoves, levelSeconds, levelMoves,
      levelSecondsOffset = 0, numHints;

  private boolean isTimerActive;
  
  private LinkedList<GamePiece> pieceList, originalPieceList;
  private HashMap<GamePiece, Integer> pieceToIndexMap;
  private HashSet<Integer> solutionSet, originalSolutionSet;
  private LightsOutPlay gamePlay;
  
  public HashMap<String, Bitmap> pieceBitmapMap; 
  
  private final Handler handler = new Handler();
  
  public GameBoard(LightsOutPlay gamePlay) {
    this.gamePlay = gamePlay;
    this.setProperties(new LinkedList<GamePiece>(), new LinkedList<GamePiece>(),
        new HashSet<Integer>(), new HashSet<Integer>(), 0, 0, 0, 0, 0, 0, 0);
    pieceBitmapMap = new HashMap<String, Bitmap>();
  }
  
  public void setProperties(LinkedList<GamePiece> pieceList,
      LinkedList<GamePiece> originalPieceList, HashSet<Integer> solutionSet,
      HashSet<Integer> originalSolutionSet,
      int totalSeconds, int totalMoves, int levelSeconds, int levelMoves, 
      int size, int level, int numHints) {
    this.setPieceList(pieceList);
    this.originalPieceList = originalPieceList;
    this.solutionSet = solutionSet;
    this.originalSolutionSet = originalSolutionSet;
    this.totalSeconds = totalSeconds;
    this.totalMoves = totalMoves;
    this.levelSeconds = levelSeconds;
    this.levelSecondsOffset = levelSeconds;
    this.levelMoves = levelMoves;
    this.size = size;
    this.level = level;
    this.numHints = numHints;
  }
  
  private void setPieceList(LinkedList<GamePiece> pieceList) {
    this.pieceList = pieceList;
    this.pieceToIndexMap = new HashMap<GamePiece, Integer>();
    if (pieceList != null) {
      for (int i = 0; i < pieceList.size(); i++) {
        this.pieceToIndexMap.put(pieceList.get(i), i);
      }
    }
  }
  
  public void restartLevel() {
    setPieceList(originalPieceList);
    
    originalPieceList = new LinkedList<GamePiece>();
    for (GamePiece gamePiece : pieceList) {
      originalPieceList.add(new GamePiece(gamePlay, this, 
          gamePiece.isLightOn(), gamePiece.isBlock()));
    }
    
    solutionSet.clear();
    for (int i : originalSolutionSet) {
      solutionSet.add(i);
    }
    
    gamePlay.showLevelStartMessage(level);
    gamePlay.playLevel(level);
  }
  
  public void startPlaying() {
    this.levelSeconds = 0;
    
    if (pieceList.size() == 0) {
      this.levelMoves = 0;
      this.levelSecondsOffset = 0;
      
      this.size = 5;
      if (level == 1) {
        this.size = 4;
      } else if (level == 6) {
        this.size = 6;
      }
      
      int numBlocks = 0;
      if (level > 1) {
        Random random = new Random();
        numBlocks = level / 2 - 1 + random.nextInt(2);
      }
      
      this.pieceBitmapMap.clear();
      for (int i = 0; i < size * size; i++) {
        GamePiece gamePiece = new GamePiece(gamePlay, this);
        this.pieceList.add(gamePiece);
        this.pieceToIndexMap.put(gamePiece, i);
      }
      
      int minMoves = level + 3;
      HashSet<Integer> blockSet = getRandomPositions(size * size, numBlocks, null);
      HashSet<Integer> positionSet = getRandomPositions(size * size, minMoves,
          blockSet);
      this.solutionSet = positionSet;
      this.originalSolutionSet = new HashSet<Integer>();
      for (int i : solutionSet) {
        originalSolutionSet.add(i);
      }
      
      for (int i : blockSet) {
        pieceList.get(i).enableBlock();
      }
      for (int i : positionSet) {
        doTogglePiece(pieceList.get(i));
      }
      
      originalPieceList.clear();
      for (GamePiece gamePiece : pieceList) {
        originalPieceList.add(new GamePiece(gamePlay, this, 
            gamePiece.isLightOn(), gamePiece.isBlock()));
      }
      
      gamePlay.showLevelStartMessage(level);
    }
    
    gamePlay.playLevel(level);
    startTimer();
  }
  
  private HashSet<Integer> getRandomPositions(int uppperLimit, int numPositions,
      Set<Integer> blockSet) {
    int numBlocks = blockSet == null ? 0 : blockSet.size();
    assert(uppperLimit > numPositions + numBlocks);
    
    Random random = new Random();
    HashSet<Integer> set = new HashSet<Integer>();
    while (set.size() < numPositions) {
      int rand = random.nextInt(uppperLimit);
      if (blockSet == null || !blockSet.contains(rand)) {
        set.add(rand);
      }
    }
    return set;
  }
  
  public void playNextLevel() {
    stopTimer();
    this.level++;
    this.pieceToIndexMap.clear();
    this.pieceList.clear();
    startPlaying();
  }
  
  public GamePiece getGamePieceByIndex(int index) {
    return this.pieceList.get(index);
  }
  
  public void startTimer() {
    isTimerActive = true;
    new Thread(new Runnable() {
      public void run() {
        final long startTime = SystemClock.uptimeMillis();
        while (isTimerActive) {
          handler.post(new Runnable() {
            public void run() {
              setLevelSeconds(((int)(SystemClock.uptimeMillis() - startTime) / 1000) 
                  + levelSecondsOffset);
            }
          });
          
          SystemClock.sleep(100);
        }
      }
    }).start();
  }
  
  public void stopTimer() {
    isTimerActive = false;
  }
  
  public void giveHint() {
    if (this.numHints == 0) {
      this.gamePlay.showHintPenaltyMessage();
    }
    this.numHints++;
    
    this.levelSecondsOffset += 30;
    this.setLevelSeconds(this.levelSeconds);
    this.setLevelMoves(this.levelMoves + 10);
    
    Object[] solutionArray = this.solutionSet.toArray();
    int rand = new Random().nextInt(solutionArray.length);
    GamePiece gamePiece = this.pieceList.get((Integer)solutionArray[rand]);
    gamePiece.activateHint();
  }
  
  public void togglePiece(GamePiece gamePiece) {
    if (gamePiece.isBlock()) {
      return;
    }
    int index = pieceToIndexMap.get(gamePiece);
    if (this.solutionSet.contains(index)) {
      this.solutionSet.remove(index);
    } else {
      this.solutionSet.add(index);
    }
    
    this.doTogglePiece(gamePiece);
    
    this.levelMoves++;
    this.gamePlay.updateMoveCount();
    
    if (testWin()) {
      stopTimer();
      totalMoves = totalMoves + levelMoves;
      totalSeconds = totalSeconds + levelSeconds;
      
      this.gamePlay.levelWon();
    }
  }
  
  private void doTogglePiece(GamePiece gamePiece) {
    gamePiece.toggleLights();
    int index = pieceToIndexMap.get(gamePiece);
    
    LinkedList<Integer> neighborIndeces = new LinkedList<Integer>();
    if (index % size != 0 && index > 0) {
      neighborIndeces.add(index - 1);
    }
    if ((index + 1) % size != 0 && index < this.pieceList.size() - 1) {
      neighborIndeces.add(index + 1);
    }
    if (index - size >= 0) {
      neighborIndeces.add(index - size);
    }
    if (index + size < this.pieceList.size()) {
      neighborIndeces.add(index + size);
    }
    
    for (int i : neighborIndeces) {
      pieceList.get(i).toggleLights();
    }
  }
  
  public boolean testWin() {
    if (pieceList.size() == 0) {
      return false;
    }
    for (GamePiece gamePiece : pieceList) {
      if (gamePiece.isLightOn()) {
        return false;
      }
    }
    return true;
  }
  
  public int getTotalSeconds() {
    return totalSeconds;
  }

  public int getTotalMoves() {
    return totalMoves;
  }

  public int getLevelSeconds() {
    return levelSeconds;
  }

  public int getLevelMoves() {
    return levelMoves;
  }
  
  public int getSize() {
    return size;
  }
  
  public int getLevel() {
    return level;
  }
  
  public int getNumHints() {
    return numHints;
  }
  
  public LinkedList<GamePiece> getPieceList() {
    return pieceList;
  }
  
  public LinkedList<GamePiece> getOriginalPieceList() {
    return originalPieceList;
  }
  
  public Set<Integer> getSolutionSet() {
    return solutionSet;
  }
  
  public Set<Integer> getOriginalSolutionSet() {
    return originalSolutionSet;
  }

  public void setLevelSeconds(int seconds) {
    this.levelSeconds = seconds;
    this.gamePlay.updateSeconds();
  }
  
  public void setLevelMoves(int moves) {
    this.levelMoves = moves;
    this.gamePlay.updateMoveCount();
  }
}
