/*SMARTFIRE: Satellite Mapping Automated Reanalysis Tool for Fire Incident REconciliation
Copyright (C) 2006-Present  USDA Forest Service AirFire Research Team and Sonoma Technology, Inc.

This program is free software: you can redistribute it and/or modify
it under the terms of the GNU General Public License as published by
the Free Software Foundation, either version 3 of the License, or
(at your option) any later version.

This program is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
GNU General Public License for more details.

You should have received a copy of the GNU General Public License
along with this program.  If not, see <https://www.gnu.org/licenses/>.
*/
package smartfire.database;

import java.io.Serializable;
import java.util.Comparator;
import java.util.Date;
import javax.persistence.*;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.joda.time.LocalDate;
import org.kohsuke.stapler.export.Exported;
import org.kohsuke.stapler.export.ExportedBean;
import smartfire.util.AreaUtil;

@ExportedBean
@Entity
@Table(name = "event_day")
public class EventDay implements SfEntity<Integer>, Serializable {
    private static final long serialVersionUID = 1L;

    public static final Comparator<EventDay> BY_DATE_ASC = new Comparator<EventDay>() {
        @Override
        public int compare(EventDay a, EventDay b) {
            return a.getEventDate().compareTo(b.getEventDate());
        }
    };
    
    @Id
    @Column(name = "id")
    @GeneratedValue(strategy=GenerationType.SEQUENCE, generator="event_day_seq_gen")
    @SequenceGenerator(name="event_day_seq_gen", sequenceName="event_day_seq")
    private Integer id;

    @Column(name = "event_date", nullable = false)
    @Temporal(javax.persistence.TemporalType.DATE)
    private Date eventDate;

    @Column(name = "daily_area", nullable = false)
    private double dailyArea;

    @ManyToOne(cascade={CascadeType.ALL})
    @JoinColumn(name="event_id", insertable=false, updatable=false)
    private Event event;

    @Override
    public Integer getId() {
        return id;
    }

    @Exported(name="date")
    public String getEventDateString() {
        return getEventDate().toString();
    }

    public LocalDate getEventDate() {
        return new DateTime(eventDate, DateTimeZone.UTC).toLocalDate();
    }

    public void setEventDate(LocalDate eventDate) {
        this.eventDate = eventDate.toDateTimeAtStartOfDay(DateTimeZone.UTC).toDate();
    }

    @Exported(name="areaAcres")
    public double getDailyAreaAcres() {
        return AreaUtil.squareMetersToAcres(dailyArea);
    }

    @Exported(name="areaMeters")
    public double getDailyArea() {
        return dailyArea;
    }

    public void setDailyArea(double dailyArea) {
        this.dailyArea = dailyArea;
    }

    Event getEvent() {
        return event;
    }

    void setEvent(Event event) {
        this.event = event;
    }
}
