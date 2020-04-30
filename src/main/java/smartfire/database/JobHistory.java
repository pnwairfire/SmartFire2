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
import java.util.Date;
import javax.persistence.*;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.joda.time.Interval;
import org.joda.time.Period;
import org.joda.time.format.PeriodFormat;
import org.joda.time.format.PeriodFormatter;
import org.kohsuke.stapler.export.Exported;
import org.kohsuke.stapler.export.ExportedBean;

/**
 * Represents previous run jobs.
 */
@ExportedBean(defaultVisibility=2)
@Entity
@Table(name = "job_history")
public class JobHistory implements SfEntity<Integer>, Serializable {
    private static final long serialVersionUID = 1L;

    @Id
    @Column(name = "id")
    @GeneratedValue(strategy=GenerationType.SEQUENCE, generator="job_history_seq_gen")
    @SequenceGenerator(name="job_history_seq_gen", sequenceName="job_history_seq")
    private Integer id;

    @Override
    public Integer getId() {
        return id;
    }

    @Column(name = "name", nullable = false, length = 100)
    private String name;

    @Column(name = "type", nullable = false)
    private String type;

    @Column(name = "status", nullable = false, length = 100)
    private String status;
    
    @Column(name = "final_status", nullable = false, length = 100)
    private String finalStatus;

    @Column(name = "start_date", nullable = false)
    @Temporal(javax.persistence.TemporalType.TIMESTAMP)
    private Date startDate;

    @Column(name = "end_date", nullable = false)
    @Temporal(javax.persistence.TemporalType.TIMESTAMP)
    private Date endDate;

    @Exported
    public String getName() {
        return name;
    }
    
    public void setName(String name) {
        this.name = name;
    }

    @Exported
    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    @Exported
    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public DateTime getStartDate() {
        return new DateTime(startDate, DateTimeZone.UTC);
    }

    @Exported(name="startDate")
    public String getStartDateString() {
        return getStartDate().toString();
    }

    public void setStartDate(DateTime startDate) {
        this.startDate = startDate.withZone(DateTimeZone.UTC).toDate();
    }

    public DateTime getEndDate() {
        return new DateTime(endDate, DateTimeZone.UTC);
    }

    @Exported(name="endDate")
    public String getEndDateString() {
        return getEndDate().toString();
    }

    public void setEndDate(DateTime endDate) {
        this.endDate = endDate.withZone(DateTimeZone.UTC).toDate();
    }

    @Exported
    public String getRunTime() {
        Period period = new Interval(this.getStartDate(), this.getEndDate()).toPeriod();
        PeriodFormatter formatter = PeriodFormat.getDefault();
        return period.toString(formatter);
    }

    public String getFinalStatus() {
        return finalStatus;
    }

    public void setFinalStatus(String finalStatus) {
        this.finalStatus = finalStatus;
    }
}
