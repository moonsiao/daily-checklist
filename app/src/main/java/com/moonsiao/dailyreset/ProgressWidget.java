package com.moonsiao.dailyreset;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.graphics.Typeface;
import android.widget.RemoteViews;

import org.json.JSONObject;

import java.util.Calendar;
import java.util.Locale;

public class ProgressWidget extends AppWidgetProvider {
    private static final String PREFS = "widget";
    private static final String KEY = "progress";

    static void save(Context ctx, String json) {
        ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().putString(KEY, json).apply();
        updateAll(ctx);
    }

    static void updateAll(Context ctx) {
        AppWidgetManager mgr = AppWidgetManager.getInstance(ctx);
        int[] ids = mgr.getAppWidgetIds(new ComponentName(ctx, ProgressWidget.class));
        if (ids != null && ids.length > 0) render(ctx, mgr, ids);
    }

    @Override
    public void onUpdate(Context ctx, AppWidgetManager mgr, int[] ids) {
        render(ctx, mgr, ids);
    }

    private static void render(Context ctx, AppWidgetManager mgr, int[] ids) {
        int done = 0, total = 0, resetHour = 0;
        double part = 0;
        try {
            SharedPreferences p = ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
            String raw = p.getString(KEY, null);
            if (raw != null) {
                JSONObject o = new JSONObject(raw);
                resetHour = o.optInt("resetHour", 0);
                String day = o.optString("day", "");
                if (day.equals(logicalDay(resetHour))) {
                    done = o.optInt("done", 0);
                    total = o.optInt("total", 0);
                    part = o.optDouble("part", 0);
                } else { // a new day started since the app was last opened
                    total = o.optInt("fixedTotal", 0);
                }
            }
        } catch (Exception ignored) { }

        Bitmap bmp = draw(done, total, part);
        Intent open = new Intent(ctx, MainActivity.class);
        PendingIntent pi = PendingIntent.getActivity(ctx, 0, open, PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT);
        for (int id : ids) {
            RemoteViews rv = new RemoteViews(ctx.getPackageName(), R.layout.widget);
            rv.setImageViewBitmap(R.id.widget_ring, bmp);
            rv.setOnClickPendingIntent(R.id.widget_root, pi);
            mgr.updateAppWidget(id, rv);
        }
        scheduleNextReset(ctx, resetHour);
    }

    private static String logicalDay(int resetHour) {
        Calendar c = Calendar.getInstance();
        c.add(Calendar.HOUR_OF_DAY, -resetHour);
        return String.format(Locale.US, "%04d-%02d-%02d", c.get(Calendar.YEAR), c.get(Calendar.MONTH) + 1, c.get(Calendar.DAY_OF_MONTH));
    }

    private static void scheduleNextReset(Context ctx, int resetHour) {
        Calendar next = Calendar.getInstance();
        next.set(Calendar.HOUR_OF_DAY, resetHour);
        next.set(Calendar.MINUTE, 0);
        next.set(Calendar.SECOND, 30);
        if (next.getTimeInMillis() <= System.currentTimeMillis()) next.add(Calendar.DAY_OF_MONTH, 1);
        Intent i = new Intent(ctx, ProgressWidget.class);
        i.setAction(AppWidgetManager.ACTION_APPWIDGET_UPDATE);
        i.putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS,
                AppWidgetManager.getInstance(ctx).getAppWidgetIds(new ComponentName(ctx, ProgressWidget.class)));
        PendingIntent pi = PendingIntent.getBroadcast(ctx, 1, i, PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT);
        AlarmManager am = (AlarmManager) ctx.getSystemService(Context.ALARM_SERVICE);
        if (am != null) am.set(AlarmManager.RTC, next.getTimeInMillis(), pi);
    }

    private static Bitmap draw(int done, int total, double part) {
        int s = 240;
        Bitmap b = Bitmap.createBitmap(s, s, Bitmap.Config.ARGB_8888);
        Canvas c = new Canvas(b);
        Paint bg = new Paint(Paint.ANTI_ALIAS_FLAG);
        bg.setColor(0xFF2563EB);
        c.drawRoundRect(new RectF(0, 0, s, s), 56, 56, bg);

        float stroke = 20f, r = 76f, cx = s / 2f, cy = s / 2f;
        RectF arc = new RectF(cx - r, cy - r, cx + r, cy + r);
        Paint track = new Paint(Paint.ANTI_ALIAS_FLAG);
        track.setStyle(Paint.Style.STROKE);
        track.setStrokeWidth(stroke);
        track.setColor(0xFF1E40AF);
        c.drawOval(arc, track);

        double frac = total > 0 ? Math.max(0, Math.min(1, part / total)) : 0;
        boolean complete = total > 0 && done >= total;
        if (complete) frac = 1;
        if (frac > 0) {
            Paint prog = new Paint(Paint.ANTI_ALIAS_FLAG);
            prog.setStyle(Paint.Style.STROKE);
            prog.setStrokeWidth(stroke);
            prog.setStrokeCap(Paint.Cap.ROUND);
            prog.setColor(0xFFFFFFFF);
            c.drawArc(arc, -90, (float) (360 * frac), false, prog);
        }

        if (complete) {
            Paint chk = new Paint(Paint.ANTI_ALIAS_FLAG);
            chk.setStyle(Paint.Style.STROKE);
            chk.setStrokeWidth(18f);
            chk.setStrokeCap(Paint.Cap.ROUND);
            chk.setStrokeJoin(Paint.Join.ROUND);
            chk.setColor(0xFFFDE047);
            Path pth = new Path();
            pth.moveTo(cx - 34, cy + 2);
            pth.lineTo(cx - 10, cy + 26);
            pth.lineTo(cx + 36, cy - 24);
            c.drawPath(pth, chk);
        } else {
            Paint t = new Paint(Paint.ANTI_ALIAS_FLAG);
            t.setColor(0xFFFFFFFF);
            t.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));
            t.setTextAlign(Paint.Align.CENTER);
            String txt = done + "/" + total;
            t.setTextSize(txt.length() > 5 ? 38f : 46f);
            Paint.FontMetrics fm = t.getFontMetrics();
            c.drawText(txt, cx, cy - (fm.ascent + fm.descent) / 2, t);
        }
        return b;
    }
}
