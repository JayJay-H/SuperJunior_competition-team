package application;

import java.net.URL;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.ResourceBundle;
import java.util.Vector;

import javafx.animation.AnimationTimer;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Orientation;
import javafx.scene.Node;
import javafx.scene.control.ListView;
import javafx.scene.control.ScrollBar;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.shape.Line;

public class TimeLineController implements Initializable {

	@FXML ListView<String> timeLineListView;
	@FXML Button timeSettingButton;
	@FXML Label currentTimeLabel;
	@FXML Line currentTimeLine;
	@FXML Label dayOfWeekLabel;
	@FXML Button leftButton; // "<"
	@FXML Button rightButton; // ">"
	
	private double 					cellHeight = 24;
	// all(list의 길이) : speed = barLength(=1) : x(barSpeed) => x = speed * barLength / all
    private double 					speed = ((144.0*cellHeight) / (24 * 60 * 60.0)) * 1.0 / (144.0*cellHeight);
    private boolean 				timerFlag = true; // 애니메이션 실행 플래그
    private boolean 				timeSetting = true; // 타임라인 고정 플래그
    private double 					currentScroll; // 현재 스크롤의 위치
    private String[]				dayOfWeekList = {"일", "월", "화", "수", "목", "금", "토"};
    private int						currentDayOfWeek; // 현재 보고있는 요일
	private ObservableList<String> 	timeLineList;
    private Vector<Integer[]> 		scheduleIndexList = new Vector<>();
    
    // 시간에 따라서 자동으로 스크롤 하는 애니메이션
    private AnimationTimer timer = new AnimationTimer() {
        private long lastUpdate = -1;
        private ScrollBar scrollbar;
        
        @Override
        public void start() {
            scrollbar = getVerticalScrollBar();
            super.start();
        }

        @Override
        public void handle(long now) {
        	if (lastUpdate < 0) {
            	lastUpdate = now;
                return;
            }

            ////////// "속도 * 시간 == 거리"를 통해 스크롤을 얼마나 이동할지 계산 //////////
            long elapsedNanos = now - lastUpdate;
            double delta = speed * elapsedNanos / 1_000_000_000;
            currentScroll += delta;
            ////////////////////////////////////////////////////////////////////

            
            ////////// 타임라인 고정 버튼이 켜져있는지 꺼져있는지에 따라 라벨 변경 //////////
            if(timerFlag) {
            	currentTimeLabel.setText("now");
            	currentTimeLine.setVisible(true);
            	scrollbar.setValue(currentScroll);
            } else {
            	currentTimeLabel.setText("");
            	currentTimeLine.setVisible(false);
            }
            ///////////////////////////////////////////////////////////////////
            
            lastUpdate = now;
        }
    };

    // listView의 scrollBar를 반환하는 메소드 (animation에 쓰기 위해)
    private ScrollBar getVerticalScrollBar() {
        ScrollBar scrollBar = null;
        for (Node node : timeLineListView.lookupAll(".scroll-bar")) {
            if (node instanceof ScrollBar) {
                scrollBar = (ScrollBar) node;
                if (scrollBar.getOrientation() == Orientation.VERTICAL) {
                    break;
                }
            }
        }
        return scrollBar;
    }
    
	@Override
	public void initialize(URL location, ResourceBundle resources) {
		////////// 각종 초기화 + 애니메이션 (타임라인) 시작 //////////
		timerFlag = true;
		timeSetting = true;
		leftButton.setDisable(true);
		rightButton.setDisable(true);
		
		timeLineListView.setFixedCellSize(cellHeight);
		timeLineList = FXCollections.observableArrayList();
		timeLineListView.setItems(timeLineList);

		Platform.runLater(() -> timer.start());
		/////////////////////////////////////////////////////
		
		
		////////// 현재 시각에 알맞은 위치에 스크롤을 향하도록 함 //////////
		Date currentDate = new Date();
		Date start = null, end = null;
		SimpleDateFormat transFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.KOREA);
		String day = transFormat.format(currentDate).split(" ")[0];
		try {
			start = transFormat.parse(day + " 00:00:00");
			end = transFormat.parse(day + " 24:00:00");
		} catch (ParseException e1) {
			e1.printStackTrace();
		}
		
		this.currentScroll = 1.0 * (currentDate.getTime() - start.getTime()) / (end.getTime() - start.getTime());
		/////////////////////////////////////////////////////////
		
		
		////////// 버튼 클릭시 타임라인 고정 및 해제 가능 //////////
		timeSettingButton.setOnMouseClicked(event -> {
			if(timeSetting) {
				timerFlag = false;
				timeSetting = false;
				leftButton.setDisable(false);
				rightButton.setDisable(false);
				timeSettingButton.setText("타임라인 고정");
			} else {
				timerFlag = true;
				timeSetting = true;
				leftButton.setDisable(true);
				rightButton.setDisable(true);
				timeSettingButton.setText("고정 해제");
				currentDayOfWeek = parseDayOfWeek((new SimpleDateFormat("E", Locale.KOREA).format(new Date())));
				showSchedules(dayOfWeekList[currentDayOfWeek]);
			}
		});
		///////////////////////////////////////////////////
		
		
		////////// 타임라인 일정 색상 표시 기능 //////////
		timeLineListView.setCellFactory(lv -> new ListCell<String>() {
			@Override
			protected void updateItem(String item, boolean empty) {
				super.updateItem(item, empty);
				if(empty) {
					setText(null);
					setStyle("");
				} else {
					setText(item);
					setStyle("");
					for(int i = 0; i < scheduleIndexList.size(); i++) {
						if(scheduleIndexList.get(i)[0] < getIndex() && scheduleIndexList.get(i)[1] > getIndex()) {
							setStyle("-fx-background-color: #A9F5F2");
						} else if(scheduleIndexList.get(i)[0] == getIndex()) {
							if(getIndex() % 2 == 0) {
								setStyle("-fx-background-color: linear-gradient(-fx-control-inner-background 50%, #A9F5F2 50%)");
							} else {
								setStyle("-fx-background-color: linear-gradient(derive(-fx-control-inner-background, -2%) 50%, #A9F5F2 50%)");
							}
						} else if(scheduleIndexList.get(i)[1] == getIndex()) {
							if(getIndex() % 2 == 0) {
								setStyle("-fx-background-color: linear-gradient(#A9F5F2 50%, -fx-control-inner-background 50%)");
							} else {
								setStyle("-fx-background-color: linear-gradient(derive(#A9F5F2, -2%) 50%, -fx-control-inner-background 50%)");
							}
						}
					}
				}
			}
		});
		///////////////////////////////////////////
		
		
		////////// 서버로부터 스케줄 String을 받아 처리하는 부분 //////////
		currentDayOfWeek = parseDayOfWeek((new SimpleDateFormat("E", Locale.KOREA).format(new Date())));
		showSchedules(dayOfWeekList[currentDayOfWeek]);
		dayOfWeekLabel.setText(dayOfWeekList[currentDayOfWeek] + "요일");
		//////////////////////////////////////////////////////////
	}
	
	public int parseDayOfWeek(String dayOfWeek) {
		int intDayOfWeek = -1;
		switch(dayOfWeek) {
		case "일":
			intDayOfWeek = 0;
			break;
		case "월":
			intDayOfWeek = 1;
			break;
		case "화":
			intDayOfWeek = 2;
			break;
		case "수":
			intDayOfWeek = 3;
			break;
		case "목":
			intDayOfWeek = 4;
			break;
		case "금":
			intDayOfWeek = 5;
			break;
		case "토":
			intDayOfWeek = 6;
			break;
		}
		return intDayOfWeek;
	}
	
	// 요일에 맞는 타임라인을 표시함
	public void showSchedules(String dayOfWeek) {
		clearSchedules();
		String line = null;
		// TODO line = Server input
		line = "일/브베 배 빵빵 연습하기/2000/2040//일/앵망 빵됴 연습하기/2100/2120//"
				+ "일/앵망 빵듀데요 연습하기/2200/2240//월/박으수/1000/1030"; // default (테스트용)
		// TODO 이 윗부분은 initialize 할 때 한 번에 하는 게 더 좋을 수도...
		
		dayOfWeekLabel.setText(dayOfWeekList[currentDayOfWeek] + "요일");
		String[] scheduleList = line.split("//");
		for(int i = 0; i < scheduleList.length; i++) {
			String[] command = scheduleList[i].split("/");
			
			if(command[0].equals(dayOfWeek)) {
				writeSchedule(command[1], command[2], command[3]);
			}
		}
	}
	
	// 타임라인 초기화
	private void clearSchedules() {
		timeLineList.clear();
		scheduleIndexList.clear();
		
		int time = 0000;
		
		timeLineList.add("");
		timeLineList.add("");
		
		for(int i = 0; i < 144; i++) {
			if(time % 100 == 0 || time % 100 == 30) {
				timeLineList.add(getTime(time));
			} else {
				timeLineList.add("");
			}
			time = addTime(time, 10);
		}
		for(int i = 0; i < 11; i++) {
			timeLineList.add("");
		}
	}
	
	// 타임라인에 스케줄 추가
	private void writeSchedule(String title, String startTime, String endTime) {
		int time = Integer.parseInt(startTime);
		int hour = time / 100;
		int min = time - hour * 100;
		int startIndex = min / 10 + hour * 6 + 2;
		timeLineList.set(startIndex, String.format("%02d:%02d", hour, min) + " " + title);
		
		time = Integer.parseInt(endTime);
		hour = time / 100;
		min = time - hour * 100;
		int endIndex = min / 10 + hour * 6 + 2;
		timeLineList.set(endIndex, String.format("%02d:%02d", hour, min));
		
		Integer[] indexList = {startIndex, endIndex};
		scheduleIndexList.add(indexList);
		for(int i = startIndex + 1; i < endIndex; i++) {
			timeLineList.set(i, "");
		}
	}
	
	private String getTime(int time) {
		int hour = time / 100;
		int min = time - hour * 100;
		
		return String.format("%02d:%02d", hour, min);
	}
	
	private int addTime(int time, int min) {
		if(time - (time / 100) * 100 > 49) {
			time -= 50;
			time += 100;
		} else {
			time += 10;
		}
		
		return time;
	}

	@FXML public void moveToLeft() {
		if(currentDayOfWeek > 0) {
			currentDayOfWeek -= 1;
		} else {
			currentDayOfWeek = dayOfWeekList.length - 1;
		}
		showSchedules(dayOfWeekList[currentDayOfWeek]);
	}

	@FXML public void moveToRight() {
		if(currentDayOfWeek < dayOfWeekList.length - 1) {
			currentDayOfWeek += 1;
		} else {
			currentDayOfWeek = 0;
		}
		showSchedules(dayOfWeekList[currentDayOfWeek]);
	}
}