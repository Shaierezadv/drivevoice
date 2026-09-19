# פקודות קול בעברית — DriveVoice MVP

## שיחה
- התקשר ליוסי
- תתקשר למיכל
- חייג 0501234567
- התקשר למספר 03-7520432

## SMS
- שלח הודעה לדני תגיע בעוד עשר דקות
- שלח SMS ליוסי אני בדרך
- הודעה למיכל מחכה בחניה

## מייל
- שלח מייל ל name@example.com נושא פגישה תוכן נתראה מחר
- מייל ל name@example.com נושא שלום תוכן היי

## פתיחת אפליקציה
- פתח ווייז
- תפתח מוזיקה
- פתח WhatsApp / וואטסאפ
- פתח מפות

## אישור / ביטול
- כן / אשר / בצע
- לא / בטל / עצור

## מבנה NLU (MVP)
Intent = CALL | SMS | EMAIL | OPEN_APP | CONFIRM | CANCEL | UNKNOWN
Entities: contactName | phoneNumber | messageBody | email | subject | appLabel
